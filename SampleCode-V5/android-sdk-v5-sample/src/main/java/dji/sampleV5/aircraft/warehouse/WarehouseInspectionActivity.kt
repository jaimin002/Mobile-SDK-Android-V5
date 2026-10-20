package dji.sampleV5.aircraft.warehouse

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.aircraft.databinding.ActivityWarehouseInspectionBinding

class WarehouseInspectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWarehouseInspectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarehouseInspectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("WarehouseInspection", "WarehouseInspectionActivity initialized with ViewBinding")

        setupListeners()
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
