package com.minar.birday.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.minar.birday.widgets.CompactWidgetProvider
import com.minar.birday.widgets.EventWidgetProvider
import com.minar.birday.widgets.MinimalWidgetProvider
import java.util.Calendar

/**
 * Fires at 00:00:30 every day to force-refresh all Birday widgets.
 * Uses setExactAndAllowWhileIdle so it runs even in Doze mode, fixing the
 * stale-widget issue caused by the imprecise updatePeriodMillis mechanism.
 *
 * Also handles BOOT_COMPLETED to restore the alarm after a reboot.
 */
class WidgetMidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MIDNIGHT_UPDATE, Intent.ACTION_BOOT_COMPLETED -> {
                // Force-update every widget provider that has active instances
                val appWidgetManager = AppWidgetManager.getInstance(context)
                for (providerClass in ALL_PROVIDERS) {
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, providerClass)
                    )
                    if (ids.isNotEmpty()) {
                        val updateIntent = Intent(context, providerClass).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(updateIntent)
                    }
                }
                // Reschedule for the next midnight
                scheduleNextMidnight(context)
            }
        }
    }

    companion object {
        const val ACTION_MIDNIGHT_UPDATE = "com.minar.birday.action.WIDGET_MIDNIGHT_UPDATE"
        private const val ALARM_REQUEST_CODE = 9900

        private val ALL_PROVIDERS = listOf(
            EventWidgetProvider::class.java,
            MinimalWidgetProvider::class.java,
            CompactWidgetProvider::class.java,
        )

        /** Schedule (or replace) the exact midnight alarm. */
        fun scheduleNextMidnight(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Target: tomorrow at 00:00:30 (30 s after midnight to safely clear the day boundary)
            val triggerAt = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 30)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val pendingIntent = buildPendingIntent(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // No exact-alarm permission on API 31-32: fall back to Doze-exempt inexact alarm.
                // This is still far better than updatePeriodMillis alone.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }

        /** Cancel the midnight alarm (call when no widgets remain). */
        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildPendingIntent(context))
        }

        /** Returns true if at least one Birday widget of any type is still active. */
        fun anyWidgetActive(context: Context): Boolean {
            val mgr = AppWidgetManager.getInstance(context)
            return ALL_PROVIDERS.any { mgr.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
        }

        private fun buildPendingIntent(context: Context) = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, WidgetMidnightReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_UPDATE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
