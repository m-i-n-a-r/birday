package com.minar.birday.utilities

import android.content.Context
import android.icu.util.Calendar
import android.icu.util.ULocale
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.minar.birday.workers.EventWorker
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


// A few seconds past the chosen minute, to stay clear of midnight
private const val CHECK_SECOND = 15

/**
 * How long until the next daily check, at the given hour and minute. The next day is a day on
 * the clock, not 24 hours: when daylight saving time begins or ends a day lasts 23 or 25 of them,
 * and 24 hours after a midnight check would land on the 23 o'clock that comes twice (#365)
 */
fun delayToNextCheck(hour: Int, minute: Int, now: ZonedDateTime = ZonedDateTime.now()): Duration {
    var next = now.toLocalDate().atTime(hour, minute, CHECK_SECOND).atZone(now.zone)
    if (!next.isAfter(now)) next = now.toLocalDate().plusDays(1)
        .atTime(hour, minute, CHECK_SECOND).atZone(now.zone)
    return Duration.between(now, next)
}

// Which notifications a daily check sends. Both, unless the additional ones have a time of their own
enum class CheckPart { ALL, MAIN, ADDITIONAL }

const val CHECK_PART_KEY = "check_part"

// The time of a daily check, read from the settings every time
fun checkTime(context: Context, part: CheckPart): Pair<Int, Int> {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val hour = prefs.getString("notification_hour", "8")!!.toInt()
    val minute = prefs.getString("notification_minute", "0")!!.toInt()
    if (part != CheckPart.ADDITIONAL) return hour to minute
    // Until a time is picked for them, the additional ones keep the main one
    val additionalHour = prefs.getString("additional_notification_hour", null)?.toIntOrNull()
    val additionalMinute = prefs.getString("additional_notification_minute", null)?.toIntOrNull()
    return (additionalHour ?: hour) to (additionalMinute ?: minute)
}

fun additionalTimeSeparate(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
    .getBoolean("additional_notification_separate", false)

// The next run of a daily check, at its own time
fun enqueueCheck(context: Context, part: CheckPart) {
    val (hour, minute) = checkTime(context, part)
    val request = OneTimeWorkRequestBuilder<EventWorker>()
        .setInitialDelay(delayToNextCheck(hour, minute).toMillis(), TimeUnit.MILLISECONDS)
        .setInputData(workDataOf(CHECK_PART_KEY to part.name))
        .build()
    WorkManager.getInstance(context).enqueue(request)
}

// Every daily check from scratch: one for all the notifications, or one each when the additional
// ones have their own time
fun scheduleChecks(context: Context) {
    // Only the checks: every work carries the name of its class as a tag, also the ones scheduled
    // by older versions, and the weekly contacts import must survive this
    WorkManager.getInstance(context).cancelAllWorkByTag(EventWorker::class.java.name)
    if (additionalTimeSeparate(context)) {
        enqueueCheck(context, CheckPart.MAIN)
        enqueueCheck(context, CheckPart.ADDITIONAL)
    } else enqueueCheck(context, CheckPart.ALL)
}

// The weekend of the region the user is in, which isn't Saturday and Sunday everywhere
fun isWeekend(date: LocalDate, locale: Locale = Locale.getDefault()): Boolean {
    val calendar = Calendar.getInstance(ULocale.forLocale(locale))
    // Midday, clear of the hour some regions start or end the weekend at
    val instant = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
    return calendar.isWeekend(Date.from(instant))
}

/**
 * The day an event on a weekend is also reminded of: the last working day before that weekend,
 * so it can be celebrated at work (#382). Null when the event isn't on a weekend
 */
fun weekendReminderDay(
    eventDate: LocalDate,
    weekend: (LocalDate) -> Boolean = { isWeekend(it) }
): LocalDate? {
    if (!weekend(eventDate)) return null
    var day = eventDate.minusDays(1)
    // A week of weekend days doesn't exist, but a broken locale must not hang the worker
    repeat(7) {
        if (!weekend(day)) return day
        day = day.minusDays(1)
    }
    return null
}
