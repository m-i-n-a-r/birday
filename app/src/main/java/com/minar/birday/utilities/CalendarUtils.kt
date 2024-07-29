package com.minar.birday.utilities

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.minar.birday.R
import java.util.TimeZone
import java.util.concurrent.TimeUnit


// Return the calendar ID, if the calendar exists
fun getCalendarId(context: Context, accountName: String, calendarDisplayName: String): Long {
    val projection = arrayOf(CalendarContract.Calendars._ID)
    val selection =
        "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(accountName, calendarDisplayName)

    context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getLong(0)
        }
    }
    return -1
}

// Create the Birday calendar or return the existing one, if it already exists
fun createOrGetCalendar(context: Context): Long {
    val accountName = context.getString(R.string.app_name)
    val calendarDisplayName = context.getString(R.string.events_notification_channel)
    var calendarId = getCalendarId(context, accountName, calendarDisplayName)

    if (calendarId == -1L) {
        // The calendar does not exist, create it
        calendarId = addCalendar(context, accountName, calendarDisplayName)
    }
    return calendarId
}

// Add a local calendar for Birday and return its ID
fun addCalendar(context: Context, accountName: String, calendarDisplayName: String): Long {
    val values = ContentValues().apply {
        put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        put(CalendarContract.Calendars.NAME, calendarDisplayName)
        put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarDisplayName)
        put(CalendarContract.Calendars.CALENDAR_COLOR, context.getColor(R.color.brownLight))
        put(
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CAL_ACCESS_OWNER
        )
        put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
        put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        put(CalendarContract.Calendars.VISIBLE, 1)
        put(CalendarContract.Calendars.SYNC_EVENTS, 1)
    }

    val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
    builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
    builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
    builder.appendQueryParameter(
        CalendarContract.Calendars.ACCOUNT_TYPE,
        CalendarContract.ACCOUNT_TYPE_LOCAL
    )

    val uri = context.contentResolver.insert(builder.build(), values)
    return uri?.lastPathSegment?.toLong() ?: -1
}

// Add an event to the local Birday calendar
fun addEvent(
    context: Context,
    calendarId: Long,
    title: String,
    description: String? = "",
    startTime: Long
): Long {
    val values = ContentValues().apply {
        put(CalendarContract.Events.DTSTART, startTime)
        put(CalendarContract.Events.DTEND, startTime + TimeUnit.DAYS.toMillis(1))
        put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.DESCRIPTION, description)
        put(CalendarContract.Events.CALENDAR_ID, calendarId)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        put(CalendarContract.Events.ALL_DAY, 1)
        put(CalendarContract.Events.RRULE, "FREQ=YEARLY") // Yearly, of course
    }

    val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    return uri?.lastPathSegment?.toLong() ?: -1
}

// Add a reminder for an event, probably unused, but could have sense in certain cases
fun addReminder(context: Context, eventId: Long, minutesBefore: Int) {
    val values = ContentValues().apply {
        put(CalendarContract.Reminders.MINUTES, minutesBefore)
        put(CalendarContract.Reminders.EVENT_ID, eventId)
        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
    }

    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
}