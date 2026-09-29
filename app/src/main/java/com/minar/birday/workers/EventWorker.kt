package com.minar.birday.workers

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.ActivityCompat
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
import com.minar.birday.receivers.NotificationActionReceiver
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.daysMilestonesEnabled
import com.minar.birday.utilities.formatDaysRemaining
import com.minar.birday.utilities.formatEventList
import com.minar.birday.utilities.formatMilestoneList
import com.minar.birday.utilities.getCircularBitmap
import com.minar.birday.utilities.getRemainingDays
import com.minar.birday.utilities.getUserBirthday
import com.minar.birday.utilities.isUserBirthday
import com.minar.birday.utilities.loadUserImage
import com.minar.birday.utilities.isDaysMilestone
import com.minar.birday.utilities.delayToNextCheck
import com.minar.birday.utilities.refreshCalendarDates
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit


// Far from the birthday ids, which never exceed the number of events of a single day
private const val MILESTONE_ID_GROUPED = 1000
private const val MILESTONE_ID_SINGLE = 2000
private const val USER_BIRTHDAY_ID = 999

class EventWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val appContext = applicationContext
        refreshCalendarDates(appContext)
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(appContext).eventDao()
        val allEvents: List<EventResult> = eventDao.getOrderedEventsStatic()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val workHour = sharedPrefs.getString("notification_hour", "8")!!.toInt()
        val workMinute = sharedPrefs.getString("notification_minute", "0")!!.toInt()
        val additionalNotificationDays =
            sharedPrefs.getStringSet("multi_additional_notification", setOf())
        val surnameFirst = sharedPrefs.getBoolean("surname_first", false)
        val hideImage = sharedPrefs.getBoolean("hide_images", false)
        val onlyFavoritesNotification = sharedPrefs.getBoolean("notification_only_favorites", false)
        val onlyFavoritesAdditional = sharedPrefs.getBoolean("additional_only_favorites", false)
        val angryBird = sharedPrefs.getBoolean("angry_bird", false)
        val groupNotification = sharedPrefs.getBoolean("grouped_notifications", true)
        val loopAvd = sharedPrefs.getBoolean("loop_avd", true)
        val milestoneNotification = daysMilestonesEnabled(applicationContext)

        try {
            // Check for upcoming and actual birthdays and send notification
            val anticipated = mutableListOf<EventResult>()
            val actual = mutableListOf<EventResult>()
            // Milestones, paired with the day they fall on: the birthday date says nothing about them
            val anticipatedMilestones = mutableListOf<Pair<EventResult, LocalDate>>()
            val actualMilestones = mutableListOf<EventResult>()
            for (event in allEvents) {
                // Same rules as the birthdays below: ignored events never, favorites only if asked
                if (milestoneNotification && event.favorite != null) {
                    additionalNotificationDays?.map { LocalDate.now().plusDays(it.toLong()) }
                        ?.firstOrNull { isDaysMilestone(event, it) }
                        ?.let {
                            if (!(onlyFavoritesAdditional && event.favorite == false))
                                anticipatedMilestones.add(event to it)
                        }
                    if (isDaysMilestone(event) && !(onlyFavoritesNotification && event.favorite == false))
                        actualMilestones.add(event)
                }

                // Fill the list of upcoming events
                if (!additionalNotificationDays.isNullOrEmpty() &&
                    additionalNotificationDays.any {
                        it.toInt() == ChronoUnit.DAYS.between(LocalDate.now(), event.nextDate)
                            .toInt()
                    }
                ) {
                    // Favorite = null means that the event is ignored
                    if (onlyFavoritesAdditional && event.favorite == false || event.favorite == null) continue
                    anticipated.add(event)
                }

                // Fill the list of events happening today
                if (event.nextDate!!.isEqual(LocalDate.now())) {
                    // Favorite = null means that the event is ignored
                    if (onlyFavoritesNotification && event.favorite == false || event.favorite == null) continue
                    actual.add(event)
                }
            }
            // Send a grouped notification, or a single notification for each event
            if (groupNotification) {
                if (anticipated.isNotEmpty()) {
                    // Send a grouped notification for each enabled additional notification
                    val groupedAnticipated = anticipated.groupBy { it.nextDate }
                    for (anticipatedList in groupedAnticipated.values)
                    // The strategy to pick a different id for each is kinda poor, but whatever
                        sendNotification(
                            anticipatedList,
                            3 + anticipated.indexOf(anticipatedList[0]),
                            surnameFirst,
                            hideImage,
                            true,
                            angryBird = angryBird,
                            disableAnimations = !loopAvd,
                            ungrouped = false
                        )
                }
                if (actual.isNotEmpty()) sendNotification(
                    actual,
                    2,
                    surnameFirst,
                    hideImage,
                    angryBird = angryBird,
                    disableAnimations = !loopAvd,
                    ungrouped = false
                )
            } else {
                // Play with the ids to make sure they are unique
                if (anticipated.isNotEmpty())
                    for (e in anticipated) {
                        sendNotification(
                            listOf(e),
                            anticipated.indexOf(e),
                            surnameFirst,
                            hideImage,
                            true,
                            angryBird = angryBird,
                            disableAnimations = !loopAvd
                        )
                    }
                if (actual.isNotEmpty())
                    for (e in actual) {
                        sendNotification(
                            listOf(e),
                            actual.indexOf(e) + anticipated.size,
                            surnameFirst,
                            hideImage,
                            false,
                            angryBird = angryBird,
                            disableAnimations = !loopAvd,
                            ungrouped = true
                        )
                    }
            }

            // The user's own birthday, from the card in the settings, at the same hour as the rest
            getUserBirthday(appContext)?.let { (name, birthday) ->
                if (isUserBirthday(birthday))
                    sendUserBirthdayNotification(name, angryBird, disableAnimations = !loopAvd, hideImage)
            }

            // Milestones get their own notifications, sent exactly like the birthdays
            if (groupNotification) {
                anticipatedMilestones.groupBy({ it.second }, { it.first }).entries
                    .forEachIndexed { index, (date, events) ->
                        sendNotification(
                            events,
                            MILESTONE_ID_GROUPED + 1 + index,
                            surnameFirst,
                            hideImage,
                            true,
                            angryBird = angryBird,
                            disableAnimations = !loopAvd,
                            ungrouped = false,
                            milestoneDate = date
                        )
                    }
                if (actualMilestones.isNotEmpty()) sendNotification(
                    actualMilestones,
                    MILESTONE_ID_GROUPED,
                    surnameFirst,
                    hideImage,
                    angryBird = angryBird,
                    disableAnimations = !loopAvd,
                    ungrouped = false,
                    milestoneDate = LocalDate.now()
                )
            } else {
                anticipatedMilestones.forEachIndexed { index, (event, date) ->
                    sendNotification(
                        listOf(event),
                        MILESTONE_ID_SINGLE + index,
                        surnameFirst,
                        hideImage,
                        true,
                        angryBird = angryBird,
                        disableAnimations = !loopAvd,
                        milestoneDate = date
                    )
                }
                actualMilestones.forEachIndexed { index, event ->
                    sendNotification(
                        listOf(event),
                        MILESTONE_ID_SINGLE + anticipatedMilestones.size + index,
                        surnameFirst,
                        hideImage,
                        false,
                        angryBird = angryBird,
                        disableAnimations = !loopAvd,
                        ungrouped = true,
                        milestoneDate = LocalDate.now()
                    )
                }
            }

            // The next check, tomorrow at the same time on the clock
            val dailyWorkRequest = OneTimeWorkRequestBuilder<EventWorker>()
                .setInitialDelay(delayToNextCheck(workHour, workMinute).toMillis(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(dailyWorkRequest)
        } catch (_: Exception) {
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
        angryBird: Boolean = false,
        disableAnimations: Boolean = false,
        ungrouped: Boolean = false,
        // Set for a days lived milestone, to the day it falls on
        milestoneDate: LocalDate? = null,
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // A milestone lists the days lived where a birthday lists the age, the rest is shared
        val eventList =
            if (milestoneDate != null)
                formatMilestoneList(nextEvents, surnameFirst, applicationContext, milestoneDate)
            else formatEventList(nextEvents, surnameFirst, applicationContext)

        // Distinguish between normal notification and upcoming birthday notification
        val notificationText =
            if (!upcoming && ungrouped) formulateNotificationText(eventList, nextEvents.size, angryBird, true)
            else if (!upcoming) formulateNotificationText(eventList, nextEvents.size, angryBird)
            else formulateAdditionalNotificationText(eventList, angryBird)

        val builder = NotificationCompat.Builder(applicationContext, "events_channel")
            .setSmallIcon(
                if (disableAnimations) R.drawable.static_notification_icon
                else if (!angryBird) R.drawable.animated_notification_icon
                else R.drawable.animated_angry_notification_icon
            )
            // Use the title to quickly distinguish between reminders and additional notifications
            .setContentTitle(
                if (upcoming) formatDaysRemaining(
                    getRemainingDays(milestoneDate ?: nextEvents[0].nextDate!!),
                    applicationContext
                ) else applicationContext.getString(R.string.notification_title)
            )
            .setContentText(notificationText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationText)
            )
            // Intent that will fire when the user taps the notification (dismiss when angryBird is disabled)
            .setContentIntent(pendingIntent)
            .setAutoCancel(!angryBird)
            .setOngoing(angryBird)

        // When the bird is angry, the notification can only be dismissed using an action
        if (angryBird) {
            // Create an Intent for the BroadcastReceiver
            val actionIntent = Intent(applicationContext, NotificationActionReceiver::class.java)
            actionIntent.putExtra("notificationId", id)
            val actionPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                id,
                actionIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            // Add the action to the notification
            builder.addAction(
                R.drawable.ic_clear_24dp,
                applicationContext.getString(android.R.string.ok),
                actionPendingIntent
            )
            // Action to open the dialer
            val phoneCall = Intent(Intent.ACTION_DIAL)
            val phonePendingIntent =
                PendingIntent.getActivity(
                    applicationContext,
                    id,
                    phoneCall,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            builder.addAction(
                R.drawable.ic_apps_dialer_24dp,
                applicationContext.getString(R.string.dialer),
                phonePendingIntent
            )
        }

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
                with(builder) {
                    // Show the bigger picture only if the text is (presumably) short
                    if (nextEvents.size == 1) {
                        val nullBitmap: Bitmap? = null
                        setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(nullBitmap)
                        )
                    }
                    setLargeIcon(getCircularBitmap(bitmap))
                }
        }
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(id, builder.build())
        }
    }

    // Best wishes to the user, same look as the event notifications
    private fun sendUserBirthdayNotification(
        name: String,
        angryBird: Boolean,
        disableAnimations: Boolean,
        hideImage: Boolean,
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val text = applicationContext.getString(R.string.notification_description_part_2)
        val builder = NotificationCompat.Builder(applicationContext, "events_channel")
            .setSmallIcon(
                if (disableAnimations) R.drawable.static_notification_icon
                else if (!angryBird) R.drawable.animated_notification_icon
                else R.drawable.animated_angry_notification_icon
            )
            .setContentTitle(applicationContext.getString(R.string.user_birthday_greeting, name))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        if (!hideImage) loadUserImage(applicationContext)?.let { builder.setLargeIcon(getCircularBitmap(it)) }
        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
            notify(USER_BIRTHDAY_ID, builder.build())
        }
    }

    // Notification for upcoming events, also considering
    private fun formulateAdditionalNotificationText(
        eventList: String,
        angryBird: Boolean = false
    ) =
        if (angryBird) "$eventList."
        else
            applicationContext.getString(R.string.additional_notification_text) + " " + eventList + ". "

    // Notification for actual events, extended if there's one event only
    private fun formulateNotificationText(
        eventList: String,
        eventCount: Int,
        angryBird: Boolean = false,
        ungrouped: Boolean = false,
    ) =
        if (angryBird || ungrouped) "$eventList."
        else {
            if (eventCount == 1)
                applicationContext.getString(R.string.notification_description_part_1) + ": " +
                        eventList + ". " + applicationContext.getString(R.string.notification_description_part_2)
            else applicationContext.getString(R.string.notification_description_part_1) + ": " +
                    eventList + ". "
        }
}
