package com.praki.makeyourphoneyours

import android.Manifest
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var dataLimitEditText: EditText
    private lateinit var timePeriodHours: EditText
    private lateinit var startServiceButton: Button
    private lateinit var configuredValuesTextView: TextView
    private lateinit var dataConsumedTextView: TextView
    private lateinit var unitRadioGroup: RadioGroup
    private lateinit var gbRadioButton: RadioButton
    private lateinit var mbRadioButton: RadioButton

    private val dataUsageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val dataConsumed = intent?.getLongExtra("dataConsumed", 0) ?: 0
            dataConsumedTextView.text = "Data Consumed: ${dataConsumed / (1024 * 1024)} MB"
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your
            // app.
        } else {
            // Explain to the user that the feature is unavailable because the
            // features requires a permission that the user has denied. At the
            // same time, respect the user's decision. Don't link to system
            // settings in an effort to convince the user to change their
            // decision.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataLimitEditText = findViewById(R.id.data_limit_gb)
        timePeriodHours = findViewById(R.id.time_period_hours)
        startServiceButton = findViewById(R.id.start_service_button)
        configuredValuesTextView = findViewById(R.id.configured_values_textview)
        dataConsumedTextView = findViewById(R.id.data_consumed_textview)
        unitRadioGroup = findViewById(R.id.unit_radio_group)
        gbRadioButton = findViewById(R.id.gb_radio_button)
        mbRadioButton = findViewById(R.id.mb_radio_button)

        val sharedPref = getSharedPreferences("DataUsagePrefs", Context.MODE_PRIVATE)
        val savedDataLimit = sharedPref.getString("dataLimit", "")
        val savedTimePeriod = sharedPref.getString("timePeriodHours", "")
        val savedUnit = sharedPref.getString("unit", "GB")

        if (savedDataLimit!!.isNotEmpty() && savedTimePeriod!!.isNotEmpty()) {
            dataLimitEditText.setText(savedDataLimit)
            timePeriodHours.setText(savedTimePeriod)
            if (savedUnit == "GB") {
                gbRadioButton.isChecked = true
            } else {
                mbRadioButton.isChecked = true
            }
            configuredValuesTextView.text =
                "Configured: $savedDataLimit $savedUnit limit for $savedTimePeriod hours"

            val dataLimit = if (savedUnit == "GB") {
                savedDataLimit.toLong() * 1024 * 1024 * 1024
            } else {
                savedDataLimit.toLong() * 1024 * 1024
            }
            val timePeriod = savedTimePeriod.toLong() * 60 * 60 * 1000

            startDataUsageService(dataLimit, timePeriod)
        }

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }

        askNotificationPermission()

        startServiceButton.setOnClickListener {
            val dataLimitString = dataLimitEditText.text.toString()
            val timePeriodString = timePeriodHours.text.toString()
            val selectedUnit = if (gbRadioButton.isChecked) "GB" else "MB"

            val dataLimit = if (selectedUnit == "GB") {
                dataLimitString.toLong() * 1024 * 1024 * 1024
            } else {
                dataLimitString.toLong() * 1024 * 1024
            }
            val timePeriod = timePeriodString.toLong() * 60 * 60 * 1000

            val sharedPref = getSharedPreferences("DataUsagePrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("dataLimit", dataLimitString)
                putString("timePeriodHours", timePeriodString)
                putString("unit", selectedUnit)
                apply()
            }

            configuredValuesTextView.text =
                "Configured: $dataLimitString $selectedUnit limit for $timePeriodString hours"

            startDataUsageService(dataLimit, timePeriod)
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK", directly request the permission.
                //       If the user selects "No thanks", allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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
        ContextCompat.registerReceiver(
            this,
            dataUsageReceiver,
            IntentFilter("com.praki.makeyourphoneyours.DATA_USAGE_UPDATE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
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