package com.praki.makeyourphoneyours

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.provider.Settings

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.net.NetworkCapabilities

class DataUsageService : Service() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var dataLimit: Long = 0
    private var timePeriod: Long = 0
    private var isWarningSoundPlayed: Boolean = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            val sharedPref = getSharedPreferences("DataUsagePrefs", Context.MODE_PRIVATE)
            dataLimit = sharedPref.getLong("dataLimit", 0)
            timePeriod = sharedPref.getLong("timePeriod", 0)
        } else {
            dataLimit = intent.getLongExtra("dataLimit", 0)
            timePeriod = intent.getLongExtra("timePeriod", 0)
            isWarningSoundPlayed = false
        }

        createNotificationChannel()
        checkDataUsage()

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                checkDataUsage()
                handler.postDelayed(this, 15 * 60 * 1000) // 15 minutes once check the data usage
            }
        }
        handler.post(runnable)

        return START_STICKY
    }

    private fun checkDataUsage() {
        val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        try {
            val networkStats = networkStatsManager.querySummaryForDevice(
                NetworkCapabilities.TRANSPORT_CELLULAR,
                null,
                System.currentTimeMillis() - timePeriod,
                System.currentTimeMillis()
            )
            val totalBytes = networkStats.rxBytes + networkStats.txBytes

            updateNotification(totalBytes)

            val intent = Intent("com.praki.makeyourphoneyours.DATA_USAGE_UPDATE").apply {
                putExtra("dataConsumed", totalBytes)
                setPackage(packageName)
            }
            sendBroadcast(intent)

            if (totalBytes > dataLimit && !isWarningSoundPlayed) {
                val mediaPlayer = MediaPlayer.create(applicationContext, R.raw.data_usage_warning)
                mediaPlayer.setOnCompletionListener { mp -> mp.release() }
                mediaPlayer.start()
                openMobileDataSettingScreen()
                isWarningSoundPlayed = true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun openMobileDataSettingScreen() {
        val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun updateNotification(dataConsumed: Long) {
        val dataLimitInMB = dataLimit / (1024 * 1024)
        val dataConsumedInMB = dataConsumed / (1024 * 1024)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "DataUsageServiceChannel")
            .setContentTitle("Data Usage Service")
            .setContentText("Used: $dataConsumedInMB MB / $dataLimitInMB MB")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setProgress(dataLimitInMB.toInt(), dataConsumedInMB.toInt(), false)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            "DataUsageServiceChannel",
            "Data Usage Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}