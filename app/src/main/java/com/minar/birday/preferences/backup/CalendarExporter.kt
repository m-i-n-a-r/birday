package com.minar.birday.preferences.backup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.addEvent
import com.minar.birday.utilities.createOrGetCalendar
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getStringForTypeCodename
import java.time.ZoneId
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
    fun exportCalendar(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for read / write calendar permissions
        val permissionWrite = act.askWriteCalendarPermission(402)
        if (!permissionWrite) return false
        val permissionRead = act.askCalendarPermission(302)
        if (!permissionRead) return false

        // Phase 1: create the Birday calendar
        var calendarId = createOrGetCalendar(act)
        if (calendarId == -1L) calendarId = createOrGetCalendar(act)
        if (calendarId == -1L) {
            context.runOnUiThread(Runnable { context.showSnackbar(context.getString(R.string.birday_export_failure)) })
            return false
        }

        // Phase 2: get every event and write it the the system calendar
        val events = act.mainViewModel.allEventsUnfiltered.value
        if (events.isNullOrEmpty()) {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            })
        }
        val writeOk = writeEventsToCalendar(act, events!!, calendarId)

        // Phase 3: check if the write event went as expected and return accordingly
        return if (writeOk) {
            context.runOnUiThread(Runnable { context.showSnackbar(context.getString(R.string.birday_export_success)) })
            true
        } else {
            context.runOnUiThread(Runnable { context.showSnackbar(context.getString(R.string.birday_export_failure)) })
            false
        }

    }

    // Write each event as an entry on the Birday calendar, in the local calendar app
    private fun writeEventsToCalendar(
        context: Context,
        events: List<EventResult>,
        calendarId: Long
    ): Boolean {
        try {
            // Always first name first, to simplify a bit, plus the event type
            for (event in events) {
                val eventName = formatName(event, false) + "- ${
                    getStringForTypeCodename(
                        context,
                        event.type!!
                    )
                }"
                addEvent(
                    context,
                    calendarId,
                    eventName,
                    event.notes,
                    event.originalDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli()
                )
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}