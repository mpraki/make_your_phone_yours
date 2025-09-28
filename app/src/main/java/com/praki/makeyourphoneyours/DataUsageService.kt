package com.praki.makeyourphoneyours

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import android.provider.Settings

class DataUsageService : Service() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var dataLimit: Long = 0
    private var timePeriod: Long = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dataLimit = intent?.getLongExtra("dataLimit", 0) ?: 0
        timePeriod = intent?.getLongExtra("timePeriod", 0) ?: 0

        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "DataUsageServiceChannel")
            .setContentTitle("Data Usage Service")
            .setContentText("Monitoring data usage...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                checkDataUsage()
                handler.postDelayed(this, timePeriod)
            }
        }
        handler.post(runnable)

        return START_STICKY
    }

    private fun checkDataUsage() {
        val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        try {
            val networkStats = networkStatsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE, 
                null, 
                System.currentTimeMillis() - timePeriod, 
                System.currentTimeMillis()
            )
            val totalBytes = networkStats.rxBytes + networkStats.txBytes

            val intent = Intent("com.praki.makeyourphoneyours.DATA_USAGE_UPDATE").apply {
                putExtra("dataConsumed", totalBytes)
                setPackage(packageName)
            }
            sendBroadcast(intent)

            if (totalBytes > dataLimit) {
                toggleMobileData(false)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun toggleMobileData(enable: Boolean) {
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val setMobileDataEnabledMethod = telephonyManager.javaClass.getDeclaredMethod(
                "setDataEnabled",
                Boolean::class.javaPrimitiveType
            )
            setMobileDataEnabledMethod.isAccessible = true
            setMobileDataEnabledMethod.invoke(telephonyManager, enable)
        } catch (e: Exception) {
            e.printStackTrace()
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "DataUsageServiceChannel",
                "Data Usage Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}