package dji.sampleV5.aircraft.warehouse

import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.ActivityWarehouseInspectionBinding
import dji.sdk.keyvalue.value.common.ComponentIndexType

class WarehouseInspectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWarehouseInspectionBinding
    private val viewModel: WarehouseInspectionViewModel by viewModels()

    private var cameraSurface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarehouseInspectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("WarehouseInspection", "WarehouseInspectionActivity initialized with ViewBinding and Camera Stream support")

        setupListeners()
        setupCameraSurface()
        observeTelemetry()
        observeVideoStream()
    }

    private fun setupCameraSurface() {
        binding.surfaceViewCameraStream.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                cameraSurface = holder.surface
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                surfaceWidth = width
                surfaceHeight = height
                cameraSurface = holder.surface
                bindSurfaceIfReady()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                unbindSurface()
                cameraSurface = null
                surfaceWidth = 0
                surfaceHeight = 0
            }
        })
    }

    private fun bindSurfaceIfReady() {
        val surface = cameraSurface
        if ((surface != null) && surface.isValid && (surfaceWidth > 0) && (surfaceHeight > 0)) {
            viewModel.putCameraStreamSurface(surface, surfaceWidth, surfaceHeight)
        }
    }

    private fun unbindSurface() {
        cameraSurface?.let { surface ->
            viewModel.removeCameraStreamSurface(surface)
            cameraSurface = null
        }
    }

    private fun observeTelemetry() {
        viewModel.telemetryState.observe(this) { state ->
            updateTelemetryUi(state)
        }
    }

    private fun observeVideoStream() {
        viewModel.activeCameraIndex.observe(this) { cameraIndex ->
            bindSurfaceIfReady()
            updateVideoStreamUi(viewModel.isStreamReceiving.value ?: false, cameraIndex)
        }

        viewModel.isStreamReceiving.observe(this) { isReceiving ->
            updateVideoStreamUi(isReceiving, viewModel.activeCameraIndex.value ?: ComponentIndexType.UNKNOWN)
        }
    }

    private fun updateVideoStreamUi(isReceiving: Boolean, cameraIndex: ComponentIndexType) {
        if (!isReceiving || cameraIndex == ComponentIndexType.UNKNOWN) {
            // No live video stream payload received yet -> NO FEED UI
            binding.layoutVideoDisconnected.visibility = View.VISIBLE
            binding.textLiveBadge.apply {
                text = "• NO FEED"
                setTextColor(ContextCompat.getColor(this@WarehouseInspectionActivity, R.color.wh_text_muted))
            }
        } else {
            // Live video payload frames genuinely receiving -> LIVE UI
            binding.layoutVideoDisconnected.visibility = View.GONE
            binding.textLiveBadge.apply {
                text = "• LIVE (${cameraIndex.name})"
                setTextColor(ContextCompat.getColor(this@WarehouseInspectionActivity, R.color.wh_status_connected))
            }
        }
    }

    private fun updateTelemetryUi(state: WarehouseTelemetryState) {
        if (!state.isConnected) {
            // Disconnected UI State
            binding.textConnectionStatusChip.apply {
                text = "• NOT CONNECTED"
                setTextColor(ContextCompat.getColor(this@WarehouseInspectionActivity, R.color.wh_status_disconnected))
                setBackgroundResource(R.drawable.bg_wh_chip_disconnected)
            }
            binding.textTelemetryBattery.text = "-- %"
            binding.textTelemetryAltitude.text = "-- m"
            binding.textTelemetrySpeed.text = "-- m/s"
            binding.textTelemetrySignal.text = "No Link"
            binding.textTelemetryGps.text = "No Fix"
            binding.textInspectionStatus.text = "Standby"
        } else {
            // Connected UI State
            binding.textConnectionStatusChip.apply {
                text = "• CONNECTED"
                setTextColor(ContextCompat.getColor(this@WarehouseInspectionActivity, R.color.wh_status_connected))
                setBackgroundResource(R.drawable.bg_wh_chip_connected)
            }
            binding.textTelemetrySignal.text = "Link Active"

            // Battery Percentage
            binding.textTelemetryBattery.text = if (state.batteryPercent != null) {
                "${state.batteryPercent} %"
            } else {
                "-- %"
            }

            // Altitude in meters
            binding.textTelemetryAltitude.text = if (state.altitudeMeters != null) {
                "%.1f m".format(state.altitudeMeters)
            } else {
                "-- m"
            }

            // Horizontal Speed in m/s
            binding.textTelemetrySpeed.text = if (state.horizontalSpeedMps != null) {
                "%.1f m/s".format(state.horizontalSpeedMps)
            } else {
                "-- m/s"
            }

            // GPS Satellites
            binding.textTelemetryGps.text = if (state.gpsSatelliteCount != null) {
                "${state.gpsSatelliteCount} GPS"
            } else {
                "No Fix"
            }

            // Flight Mode / Status
            binding.textInspectionStatus.text = state.flightMode
        }
    }

    private fun setupListeners() {
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonSettings.setOnClickListener {
            showToast("Settings — Standby Mode")
        }

        binding.buttonStartInspection.setOnClickListener {
            showToast("Aircraft disconnected. Connect drone to start inspection mission.")
        }

        binding.buttonViewFindings.setOnClickListener {
            showToast("No active findings recorded in current session.")
        }

        binding.buttonTakeoff.setOnClickListener {
            showToast("Flight controls disabled — aircraft disconnected.")
        }

        binding.buttonRTH.setOnClickListener {
            showToast("Flight controls disabled — aircraft disconnected.")
        }

        binding.buttonEmergencyStop.setOnClickListener {
            showToast("Flight controls disabled — aircraft disconnected.")
        }

        binding.dpadContainer.setOnClickListener {
            showToast("Joystick controls disabled — aircraft disconnected.")
        }
    }

    override fun onPause() {
        super.onPause()
        unbindSurface()
    }

    override fun onStop() {
        super.onStop()
        unbindSurface()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
