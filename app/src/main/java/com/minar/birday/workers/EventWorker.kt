package com.minar.birday.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.minar.birday.R
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventResult
import com.minar.birday.utilities.SplashActivity
import java.lang.Exception
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit

class EventWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val eventDao: EventDao = EventDatabase.getBirdayDataBase(appContext)!!.eventDao()
        val nextEvents: List<EventResult> = eventDao.getOrderedNextEvents()
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val workHour = sp.getString("notification_hour", "8")!!.toInt()

        try {
            // Send notification
            if (!nextEvents.isNullOrEmpty() && nextEvents[0].nextDate!!.isEqual(LocalDate.now())) sendNotification(nextEvents)

            // Set Execution at the time specified
            dueDate.set(Calendar.HOUR_OF_DAY, workHour)
            dueDate.set(Calendar.MINUTE, 0)
            dueDate.set(Calendar.SECOND, 0)
            if (dueDate.before(currentDate)) dueDate.add(Calendar.HOUR_OF_DAY, 24)
            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
            val dailyWorkRequest = OneTimeWorkRequestBuilder<EventWorker>()
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(dailyWorkRequest)
        }
        catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    // Send notification if there's one or more birthdays today
    private fun sendNotification(nextEvents: List<EventResult>) {
        val intent = Intent(applicationContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, 0)

        val notificationText = formulateNotificationText(nextEvents)
        val builder = NotificationCompat.Builder(applicationContext, "events_channel")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notificationText))
            // Intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) { notify(1, builder.build()) }
    }

    private fun formulateNotificationText(nextEvents: List<EventResult>): String {
        var response = applicationContext.getString(R.string.notification_description_part_1) + ": "
        nextEvents.forEach {
            if (nextEvents.indexOf(it) == 0) response += it.name + ", " +
                    it.nextDate?.year?.minus(it.originalDate.year) + " " + applicationContext.getString(R.string.years)
            if (nextEvents.indexOf(it) in 1..2) response += ", " + it.name + ", " +
                    it.nextDate?.year?.minus(it.originalDate.year) + " " + applicationContext.getString(R.string.years)
            if (nextEvents.indexOf(it) == 3) response += ", " + applicationContext.getString(R.string.event_others)
        }
        response += ". " + applicationContext.getString(R.string.notification_description_part_2)

        return response
    }
}