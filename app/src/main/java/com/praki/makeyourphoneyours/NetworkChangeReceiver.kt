package com.praki.makeyourphoneyours

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isMobileData = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val serviceIntent = Intent(context, DataUsageService::class.java)
        if (isMobileData) {
            val sharedPref = context.getSharedPreferences("DataUsagePrefs", Context.MODE_PRIVATE)
            val dataLimit = sharedPref.getLong("dataLimit", 0)
            val timePeriod = sharedPref.getLong("timePeriod", 0)
            if (dataLimit > 0 && timePeriod > 0) {
                serviceIntent.putExtra("dataLimit", dataLimit)
                serviceIntent.putExtra("timePeriod", timePeriod)
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(serviceIntent)
        }
    }
}
