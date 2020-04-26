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
import java.util.*
import java.util.concurrent.TimeUnit

class EventWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val eventDao: EventDao = EventDatabase.getBirdayDataBase(appContext)!!.eventDao();
        val nextEvents: List<EventResult> = eventDao.getOrderedNextEvents()
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        val sp = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val workHour = sp.getInt("notification_hour", 8)

        // Send notification if there's a birthday today TODO for testing purposes, the notification is sent anyway
        val intent = Intent(appContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, 0)

        val builder = NotificationCompat.Builder(applicationContext, "events_channel")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("Birday intensifies")
            .setContentText("Birday is sending this notification for testing purposes, just ignore it, thanks for your cooperation")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Birday is sending this notification for testing purposes, just ignore it, thanks for your cooperation"))
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(applicationContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }

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
        return Result.success()
    }
}