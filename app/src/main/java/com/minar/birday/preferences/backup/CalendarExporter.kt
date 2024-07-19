package com.minar.birday.preferences.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventResult
import kotlin.concurrent.thread


class CalendarExporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        v.setOnClickListener(null)
        act.vibrate()
        // Only export if there's at least one event
        if (act.mainViewModel.allEventsUnfiltered.value.isNullOrEmpty()) {
            act.showSnackbar(context.getString(R.string.no_events))
        } else {
            thread {
                exportCalendar(context)
                (context as MainActivity).runOnUiThread {
                    v.setOnClickListener(this)
                }
            }
        }
    }

    // Import the contacts from device contacts (not necessarily Google)
    private fun exportCalendar(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for write calendar permission
        val permission = act.askWriteCalendarPermission(402)
        if (!permission) return false

        // Phase 1: create the Birday calendar
        val created = createBirdayCalendar(act)
        if (created == 0L) return false

        // Phase 2: get every event and write it the the system calendar
        val events = act.mainViewModel.allEventsUnfiltered.value
        if (events.isNullOrEmpty()) {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            })
        }
        val writeOk = writeEventsToCalendar(act, events!!)

        // Phase 3: check if the write event went as expected and return accordingly
        return if (writeOk) {
            context.runOnUiThread(Runnable { context.showSnackbar(context.getString(R.string.birday_export_success)) })
            true
        } else {
            context.runOnUiThread(Runnable { context.showSnackbar(context.getString(R.string.birday_export_failure)) })
            false
        }

    }

    // Get the contacts and save them in a map
    private fun writeEventsToCalendar(context: Context, events: List<EventResult>): Boolean {
        try {
            for (event in events) {
                val intent = Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                    .putExtra(CalendarContract.Events.TITLE, "Yoga")
                    .putExtra(CalendarContract.Events.DESCRIPTION, "Group class")
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, "The gym")
                    .putExtra(
                        CalendarContract.Events.AVAILABILITY,
                        CalendarContract.Events.AVAILABILITY_FREE
                    )

                    .putExtra(Intent.EXTRA_EMAIL, "rowan@example.com,trevor@example.com")
                context.startActivity(intent)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // Create a calendar to store the events from Birday
    private fun createBirdayCalendar(context: Context): Long {
        var calUri = CalendarContract.Calendars.CONTENT_URI
        val cv = ContentValues()
        cv.put(CalendarContract.Calendars.ACCOUNT_NAME, "Birday")
        cv.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        cv.put(CalendarContract.Calendars.NAME, "Birday")
        cv.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Birday events")
        cv.put(CalendarContract.Calendars.CALENDAR_COLOR, context.getColor(R.color.brownLight))
        cv.put(
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CAL_ACCESS_OWNER
        )
        cv.put(CalendarContract.Calendars.OWNER_ACCOUNT, true)
        cv.put(CalendarContract.Calendars.VISIBLE, 1)
        cv.put(CalendarContract.Calendars.SYNC_EVENTS, 1)

        calUri = calUri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "Birday")
            .appendQueryParameter(
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.ACCOUNT_TYPE_LOCAL
            )
            .build()
        val resolver = context.contentResolver
        val result: Uri? = resolver.insert(calUri, cv)
        return if (result != null) {
            result.lastPathSegment!!.toLong()
        } else 0L
    }
}