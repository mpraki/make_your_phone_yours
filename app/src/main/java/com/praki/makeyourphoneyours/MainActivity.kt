package com.praki.makeyourphoneyours

import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dataLimitGb: EditText
    private lateinit var timePeriodHours: EditText
    private lateinit var startServiceButton: Button
    private lateinit var configuredValuesTextView: TextView
    private lateinit var dataConsumedTextView: TextView

    private val dataUsageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val dataConsumed = intent?.getLongExtra("dataConsumed", 0) ?: 0
            dataConsumedTextView.text = "Data Consumed: ${dataConsumed / (1024 * 1024)} MB"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataLimitGb = findViewById(R.id.data_limit_gb)
        timePeriodHours = findViewById(R.id.time_period_hours)
        startServiceButton = findViewById(R.id.start_service_button)
        configuredValuesTextView = findViewById(R.id.configured_values_textview)
        dataConsumedTextView = findViewById(R.id.data_consumed_textview)

        val sharedPref = getSharedPreferences("DataUsagePrefs", Context.MODE_PRIVATE)
        val savedDataLimit = sharedPref.getString("dataLimitGb", "")
        val savedTimePeriod = sharedPref.getString("timePeriodHours", "")

        if (savedDataLimit!!.isNotEmpty() && savedTimePeriod!!.isNotEmpty()) {
            dataLimitGb.setText(savedDataLimit)
            timePeriodHours.setText(savedTimePeriod)
            configuredValuesTextView.text = "Configured: $savedDataLimit GB limit for $savedTimePeriod hours"

            val dataLimit = savedDataLimit.toLong() * 1024 * 1024 * 1024
            val timePeriod = savedTimePeriod.toLong() * 60 * 60 * 1000

            startDataUsageService(dataLimit, timePeriod)
        }

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }

        startServiceButton.setOnClickListener {
            val dataLimit = dataLimitGb.text.toString().toLong() * 1024 * 1024 * 1024
            val timePeriod = timePeriodHours.text.toString().toLong() * 60 * 60 * 1000

            val sharedPref = getSharedPreferences("DataUsagePrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("dataLimitGb", dataLimitGb.text.toString())
                putString("timePeriodHours", timePeriodHours.text.toString())
                apply()
            }

            configuredValuesTextView.text = "Configured: ${dataLimitGb.text} GB limit for ${timePeriodHours.text} hours"

            startDataUsageService(dataLimit, timePeriod)
        }
    }

    private fun startDataUsageService(dataLimit: Long, timePeriod: Long) {
        val intent = Intent(this, DataUsageService::class.java).apply {
            putExtra("dataLimit", dataLimit)
            putExtra("timePeriod", timePeriod)
        }
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(this, dataUsageReceiver, IntentFilter("com.praki.makeyourphoneyours.DATA_USAGE_UPDATE"), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(dataUsageReceiver)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
}