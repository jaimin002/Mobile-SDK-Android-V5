package dji.sampleV5.aircraft.warehouse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.models.DJIViewModel
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.utils.common.LogUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * ViewModel for managing and exposing aircraft telemetry state in the warehouse inspection module.
 *
 * Listens to DJI SDK V5 KeyManager callbacks, converts raw SDK telemetry into immutable
 * [WarehouseTelemetryState] values, and posts updates to a LiveData stream in a thread-safe manner.
 */
class WarehouseInspectionViewModel : DJIViewModel() {

    val tag: String = LogUtils.getTag(this)

    @Volatile
    private var currentTelemetryState = WarehouseTelemetryState()

    private val _telemetryState = MutableLiveData(currentTelemetryState)
    val telemetryState: LiveData<WarehouseTelemetryState> = _telemetryState

    private val isInitialized = AtomicBoolean(false)

    init {
        initTelemetryListeners()
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
                // Reset telemetry readings when aircraft disconnects
                updateTelemetry { WarehouseTelemetryState(isConnected = false) }
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

    override fun onCleared() {
        super.onCleared()
        KeyManager.getInstance().cancelListen(this)
        isInitialized.set(false)
    }
}
