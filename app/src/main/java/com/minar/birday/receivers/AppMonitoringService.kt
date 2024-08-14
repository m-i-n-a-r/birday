package com.minar.birday.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.minar.birday.R

class AppMonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            if (isDndEnabled()) {
                checkRunningAppsAndHandleCall()
            }
            handler.postDelayed(this, INTERVAL) // INTERVAL in milliseconds
        }
    }

    companion object {
        private const val INTERVAL: Long = 40000 // 40 seconds
        private const val CHANNEL_ID = "app_monitoring_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DND Enabled")
            .setContentText("Monitoring meeting apps.")
            .setSmallIcon(R.drawable.birday_logo)
            .build()
        startForeground(1, notification)
        handler.post(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkRunningAppsAndHandleCall() {
        val dndApps = listOf("us.zoom.videomeetings", "com.microsoft.teams")

        val packageName = getForegroundAppPackageName()
        Log.d("CurrentApp3", "Foreground app: $packageName")

        for(app_name in dndApps){
            if(app_name == packageName){
                Log.d("CheckRunningApps", "DND app detected: ${packageName}")

                setUpCallListener()
                break
            }
        }
    }


    private fun setUpCallListener() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val listener = MyPhoneStateListener(this)
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun isDndEnabled(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }
    }

    private fun getForegroundAppPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 // Check usage for the last 1 minute

        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        if (usageStatsList.isNullOrEmpty()) {
            Log.d("UsageStats", "No usage stats available for the given time range.")
            return null
        }

        // Get the most recent app used within the given time frame
        val recentUsageStats = usageStatsList.maxByOrNull { it.lastTimeUsed }
        return recentUsageStats?.packageName
    }

}
