package com.minar.birday.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getCircularBitmap
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit


class EventWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(appContext).eventDao()
        val allEvents: List<EventResult> = eventDao.getOrderedEventsStatic()
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val workHour = sharedPrefs.getString("notification_hour", "8")!!.toInt()
        val workMinute = sharedPrefs.getString("notification_minute", "0")!!.toInt()
        val additionalNotification = sharedPrefs.getString("additional_notification", "0")!!.toInt()
        val surnameFirst = sharedPrefs.getBoolean("surname_first", false)
        val hideImage = sharedPrefs.getBoolean("hide_images", false)
        val onlyFavorites = sharedPrefs.getBoolean("notification_only_favorites", false)

        try {
            // Check for upcoming and actual birthdays and send notification
            val anticipated = mutableListOf<EventResult>()
            val actual = mutableListOf<EventResult>()
            for (event in allEvents) {
                // Send a notification considering the only favorites option
                if (onlyFavorites && event.favorite == false) continue
                if (additionalNotification != 0 && ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        event.nextDate
                    ).toInt() == additionalNotification
                )
                    anticipated.add(event)
                if (event.nextDate!!.isEqual(LocalDate.now()))
                    actual.add(event)
            }
            if (anticipated.isNotEmpty()) sendNotification(
                anticipated,
                1,
                surnameFirst,
                hideImage,
                true,
            )
            if (actual.isNotEmpty()) sendNotification(
                actual,
                2,
                surnameFirst,
                hideImage,
            )

            // Set Execution at the time specified + 15 seconds to avoid midnight problems
            dueDate.set(Calendar.HOUR_OF_DAY, workHour)
            dueDate.set(Calendar.MINUTE, workMinute)
            dueDate.set(Calendar.SECOND, 15)
            if (dueDate.before(currentDate)) dueDate.add(Calendar.HOUR_OF_DAY, 24)
            val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
            val dailyWorkRequest = OneTimeWorkRequestBuilder<EventWorker>()
                .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(dailyWorkRequest)
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    // Send notification if there's one or more birthdays today
    private fun sendNotification(
        nextEvents: List<EventResult>,
        id: Int,
        surnameFirst: Boolean,
        hideImage: Boolean,
        upcoming: Boolean = false,
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Distinguish between normal notification and upcoming birthday notification
        val notificationText = if (!upcoming) formulateNotificationText(nextEvents, surnameFirst)
        else formulateAdditionalNotificationText(nextEvents, surnameFirst)

        val builder = NotificationCompat.Builder(applicationContext, "events_channel")
            .setSmallIcon(R.drawable.animated_notification_icon)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            // Intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // If the images are shown, show the first image available in two ways
        if (!hideImage) {
            var bitmap: Bitmap? = null
            // Check if any event has an image
            for (event in nextEvents) {
                if (event.image != null)
                    bitmap = byteArrayToBitmap(event.image)
                if (bitmap != null) break
            }
            // If an image was found, set the appropriate style
            if (bitmap != null)
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null)
                )
                    .setLargeIcon(getCircularBitmap(bitmap))
        }

        with(NotificationManagerCompat.from(applicationContext)) { notify(id, builder.build()) }
    }

    // Notification for upcoming events, also considering
    private fun formulateAdditionalNotificationText(
        nextEvents: List<EventResult>,
        surnameFirst: Boolean
    ) =
        applicationContext.getString(R.string.additional_notification_text) + " " + formatEventList(
            nextEvents, surnameFirst
        ) + ". "

    // Notification for actual events
    private fun formulateNotificationText(nextEvents: List<EventResult>, surnameFirst: Boolean) =
        applicationContext.getString(R.string.notification_description_part_1) + ": " + formatEventList(
            nextEvents, surnameFirst
        ) + ". " + applicationContext.getString(R.string.notification_description_part_2)

    // Given a series of events, format them considering the yearMatters parameter and the number
    private fun formatEventList(events: List<EventResult>, surnameFirst: Boolean): String {
        var formattedEventList = ""
        events.forEach {
            // Years. They're not used in the string if the year doesn't matter
            val years = it.nextDate?.year?.minus(it.originalDate.year)!!
            // Only the data of the first 3 events are displayed
            if (events.indexOf(it) in 0..2) {
                // If the event is not the first, add an extra comma
                if (events.indexOf(it) != 0) formattedEventList += ", "
                // Show the last name, if any, if there's only one event
                formattedEventList += if (events.size == 1)
                    formatName(it, surnameFirst)
                else it.name
                // If the year is considered, display it. Else only display the name
                if (it.yearMatter!!) formattedEventList += ", " +
                        applicationContext.resources.getQuantityString(
                            R.plurals.years,
                            years,
                            years
                        )
            }
            // If more than 3 events, just let the user know other events are in the list
            if (events.indexOf(it) == 3) ", " + applicationContext.getString(R.string.event_others)
        }
        return formattedEventList
    }
}
