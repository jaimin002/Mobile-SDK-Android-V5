package dji.sampleV5.aircraft.warehouse

import android.view.Surface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.models.DJIViewModel
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.utils.common.LogUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * ViewModel for managing and exposing aircraft telemetry state and camera streaming in the warehouse inspection module.
 *
 * Listens to DJI SDK V5 KeyManager callbacks and MediaDataCenter camera stream updates, converts raw SDK telemetry into
 * immutable [WarehouseTelemetryState] values, and manages camera stream surface binding in a thread-safe manner.
 */
class WarehouseInspectionViewModel : DJIViewModel(), ICameraStreamManager.AvailableCameraUpdatedListener {

    val tag: String = LogUtils.getTag(this)

    @Volatile
    private var currentTelemetryState = WarehouseTelemetryState()

    private val _telemetryState = MutableLiveData(currentTelemetryState)
    val telemetryState: LiveData<WarehouseTelemetryState> = _telemetryState

    private val _availableCameraList = MutableLiveData<List<ComponentIndexType>>(emptyList())
    val availableCameraList: LiveData<List<ComponentIndexType>> = _availableCameraList

    private val _activeCameraIndex = MutableLiveData(ComponentIndexType.UNKNOWN)
    val activeCameraIndex: LiveData<ComponentIndexType> = _activeCameraIndex

    private val _isStreamReceiving = MutableLiveData(false)
    val isStreamReceiving: LiveData<Boolean> = _isStreamReceiving

    private val isInitialized = AtomicBoolean(false)
    private var registeredSurface: Surface? = null
    private var listeningCameraIndex = ComponentIndexType.UNKNOWN

    private val receiveStreamListener = ICameraStreamManager.ReceiveStreamListener { _, _, _, _ ->
        // Triggered when real video payload packets arrive from the aircraft
        if (_isStreamReceiving.value != true) {
            _isStreamReceiving.postValue(true)
        }
    }

    init {
        initTelemetryListeners()
        initCameraStreamListeners()
    }

    /**
     * Atomically updates [currentTelemetryState] and posts the result to [_telemetryState].
     * Prevents race conditions when multiple background callbacks fire in quick succession.
     */
    private fun updateTelemetry(transform: (WarehouseTelemetryState) -> WarehouseTelemetryState) {
        val newState = synchronized(this) {
            val updated = transform(currentTelemetryState)
            currentTelemetryState = updated
            updated
        }
        _telemetryState.postValue(newState)
    }

    /**
     * Initializes SDK KeyManager listeners for real-time telemetry updates.
     * Guaranteed to register listeners only once.
     */
    fun initTelemetryListeners() {
        if (isInitialized.getAndSet(true)) {
            return
        }

        // 1. Aircraft Connection Status
        FlightControllerKey.KeyConnection.create().listen(this) { connected ->
            val isConnected = connected ?: false
            if (!isConnected) {
                // Reset telemetry readings & active camera when aircraft disconnects
                updateTelemetry { WarehouseTelemetryState(isConnected = false) }
                _availableCameraList.postValue(emptyList())
                updateActiveCamera(ComponentIndexType.UNKNOWN)
            } else {
                updateTelemetry { it.copy(isConnected = true) }
            }
        }

        // 2. Battery Percentage
        BatteryKey.KeyChargeRemainingInPercent.create().listen(this) { batteryPercent ->
            updateTelemetry { it.copy(batteryPercent = batteryPercent) }
        }

        // 3. Altitude (Meters)
        FlightControllerKey.KeyAltitude.create().listen(this) { altitude ->
            updateTelemetry { it.copy(altitudeMeters = altitude) }
        }

        // 4. Horizontal Speed (Meters per second)
        FlightControllerKey.KeyAircraftVelocity.create().listen(this) { velocity3D ->
            val speed = if (velocity3D != null) {
                val vx = velocity3D.x ?: 0.0
                val vy = velocity3D.y ?: 0.0
                sqrt((vx * vx) + (vy * vy))
            } else {
                null
            }
            updateTelemetry { it.copy(horizontalSpeedMps = speed) }
        }

        // 5. GPS Satellite Count
        FlightControllerKey.KeyGPSSatelliteCount.create().listen(this) { satellites ->
            updateTelemetry { it.copy(gpsSatelliteCount = satellites) }
        }

        // 6. Flight Mode
        FlightControllerKey.KeyFlightModeString.create().listen(this) { modeStr ->
            val mode = modeStr ?: "UNKNOWN"
            updateTelemetry { it.copy(flightMode = mode) }
        }
    }

    /**
     * Registers listener for available camera streams from [MediaDataCenter].
     */
    private fun initCameraStreamListeners() {
        MediaDataCenter.getInstance().cameraStreamManager.addAvailableCameraUpdatedListener(this)
    }

    override fun onAvailableCameraUpdated(availableCameraList: List<ComponentIndexType>) {
        _availableCameraList.postValue(availableCameraList)

        // Camera selection strategy: Prefer LEFT_OR_MAIN (primary payload), fallback to first available camera, else UNKNOWN
        val selectedCamera = availableCameraList.firstOrNull { it == ComponentIndexType.LEFT_OR_MAIN }
            ?: availableCameraList.firstOrNull()
            ?: ComponentIndexType.UNKNOWN

        updateActiveCamera(selectedCamera)
    }

    override fun onCameraStreamEnableUpdate(cameraStreamEnableMap: MutableMap<ComponentIndexType, Boolean>) {
        // Reserved for future camera channel enable state updates
    }

    private fun updateActiveCamera(cameraIndex: ComponentIndexType) {
        if (listeningCameraIndex != ComponentIndexType.UNKNOWN) {
            MediaDataCenter.getInstance().cameraStreamManager.removeReceiveStreamListener(receiveStreamListener)
            listeningCameraIndex = ComponentIndexType.UNKNOWN
        }

        _activeCameraIndex.postValue(cameraIndex)
        _isStreamReceiving.postValue(false)

        if (cameraIndex != ComponentIndexType.UNKNOWN) {
            listeningCameraIndex = cameraIndex
            MediaDataCenter.getInstance().cameraStreamManager.addReceiveStreamListener(cameraIndex, receiveStreamListener)
        }
    }

    /**
     * Binds a native Android [Surface] to the active camera stream.
     */
    fun putCameraStreamSurface(
        surface: Surface,
        width: Int,
        height: Int,
        scaleType: ICameraStreamManager.ScaleType = ICameraStreamManager.ScaleType.CENTER_INSIDE,
    ) {
        val cameraIndex = _activeCameraIndex.value ?: ComponentIndexType.UNKNOWN
        if ((cameraIndex != ComponentIndexType.UNKNOWN) && surface.isValid) {
            if ((registeredSurface != null) && (registeredSurface != surface)) {
                MediaDataCenter.getInstance().cameraStreamManager.removeCameraStreamSurface(registeredSurface!!)
            }
            registeredSurface = surface
            MediaDataCenter.getInstance().cameraStreamManager.putCameraStreamSurface(
                cameraIndex,
                surface,
                width,
                height,
                scaleType,
            )
        }
    }

    /**
     * Unbinds a native Android [Surface] from the SDK camera stream manager.
     * Only removes if the surface matches the currently registered surface.
     */
    fun removeCameraStreamSurface(surface: Surface) {
        if (registeredSurface == surface) {
            MediaDataCenter.getInstance().cameraStreamManager.removeCameraStreamSurface(surface)
            registeredSurface = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        KeyManager.getInstance().cancelListen(this)
        MediaDataCenter.getInstance().cameraStreamManager.removeAvailableCameraUpdatedListener(this)
        if (listeningCameraIndex != ComponentIndexType.UNKNOWN) {
            MediaDataCenter.getInstance().cameraStreamManager.removeReceiveStreamListener(receiveStreamListener)
            listeningCameraIndex = ComponentIndexType.UNKNOWN
        }
        registeredSurface?.let {
            MediaDataCenter.getInstance().cameraStreamManager.removeCameraStreamSurface(it)
            registeredSurface = null
        }
        isInitialized.set(false)
    }
}
