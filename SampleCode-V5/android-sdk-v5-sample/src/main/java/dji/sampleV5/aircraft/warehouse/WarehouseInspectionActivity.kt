
package dji.sampleV5.aircraft.warehouse

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.aircraft.R

class WarehouseInspectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("WarehouseInspection", "WarehouseInspectionActivity created")
        setContentView(R.layout.activity_warehouse_inspection)
    }
}
