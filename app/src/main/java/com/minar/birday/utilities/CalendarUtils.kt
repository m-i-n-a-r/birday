package com.minar.birday.utilities

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.minar.birday.R
import java.time.LocalDate
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
    Log.d("export", "Exporting to calendar: $title with start time $startTime")
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
    // Stop if the event already exists
    if (isEventDuplicate(context, title, startTime)) {
        Log.d("export", "Duplicate found in calendar: $title")
        return -1
    }

    // Else insert it and return its ID
    val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
    return uri?.lastPathSegment?.toLong() ?: -1
}

// Check if an event is already in the calendar by name and date. Naive, but whatever
fun isEventDuplicate(
    context: Context,
    title: String,
    eventStartTime: Long
): Boolean {
    val resolver: ContentResolver = context.contentResolver
    val uri: Uri = CalendarContract.Events.CONTENT_URI
    val selection =
        "((${CalendarContract.Events.TITLE} = ?))"
    val selectionArgs = arrayOf(title)

    val cursor: Cursor? = resolver.query(uri, null, selection, selectionArgs, null)
    return cursor?.use { it.count > 0 } ?: false
}

// Fucking nuke the local calendar and send it directly to hell
fun deleteLocalCalendar(context: Context, calendarName: String) {
    val cr: ContentResolver = context.contentResolver
    val uri: Uri = CalendarContract.Calendars.CONTENT_URI
    val selection = "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(calendarName)

    // Obtain the calendar ID
    val cursor = cr.query(uri, arrayOf(CalendarContract.Calendars._ID), selection, selectionArgs, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val calendarId = it.getLong(0)
            val deleteUri = ContentUris.withAppendedId(uri, calendarId)
            val rowsDeleted = cr.delete(deleteUri, null, null)
            if (rowsDeleted > 0) {
                Log.d("calendar","Calendar nuked, get rekt")
            } else {
                Log.d("calendar", "Can't find the local calendar")
            }
        }
    }
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