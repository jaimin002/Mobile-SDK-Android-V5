package dji.sampleV5.aircraft.warehouse

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.ActivityWarehouseInspectionBinding

class WarehouseInspectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWarehouseInspectionBinding
    private val viewModel: WarehouseInspectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarehouseInspectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("WarehouseInspection", "WarehouseInspectionActivity initialized with ViewBinding and ViewModel")

        setupListeners()
        observeTelemetry()
    }

    private fun observeTelemetry() {
        viewModel.telemetryState.observe(this) { state ->
            updateTelemetryUi(state)
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
