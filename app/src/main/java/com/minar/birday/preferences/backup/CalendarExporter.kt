package com.minar.birday.preferences.backup

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.model.ImportedEvent
import com.minar.birday.utilities.shareFile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
        // Ask for contacts permission
        val permission = act.askWriteCalendarPermission(402)
        if (!permission) return false

        // Phase 1: get every event and write it the the system calendar
        val events = act.mainViewModel.allEventsUnfiltered.value
        val calendarEvents = writeEventsToCalendar(events!!)

        // Phase 2: convert the extracted data in an Event List, verify duplicates
        // Phase 3: insert the remaining events in the db and update the recycler
        return if (events.size == 0) {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            })
            true
        } else {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_success))
            })
            true
        }
    }

    // Get the contacts and save them in a map
    private fun writeEventsToCalendar(events: List<EventResult>) {

    }
}