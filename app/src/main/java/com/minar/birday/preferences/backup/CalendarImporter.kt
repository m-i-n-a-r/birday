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
import com.minar.birday.model.ImportedEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread


class CalendarImporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
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
        thread {
            importCalendar(context)
            (context as MainActivity).runOnUiThread {
                v.setOnClickListener(this)
            }
        }
    }

    // Import the contacts from device contacts (not necessarily Google)
    private fun importCalendar(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for contacts permission
        val permission = act.askCalendarPermission(302)
        if (!permission) return false

        // Phase 1: get every calendar event having at least a name and a date
        val calendarEvents = getCalendarEvents()
        if (calendarEvents.isEmpty()) {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            })
            return true
        }

        // Phase 2: convert the extracted data in an Event List, verify duplicates
        val events = mutableListOf<Event>()
        loop@ for (calendarEvent in calendarEvents) {
            var date: LocalDate
            val countYear = false
            val notes = calendarEvent.customLabel

            try {
                // Missing year, simply don't consider the year exactly like the contacts app does
                val parseDate = calendarEvent.eventDate
                date = LocalDate.parse(parseDate)
            } catch (e: Exception) {
                continue
            }
            val event = Event(
                id = 0,
                name = calendarEvent.completeName,
                surname = "",
                originalDate = date,
                yearMatter = countYear,
                type = calendarEvent.eventType,
                image = calendarEvent.image,
                notes = notes
            )
            events.add(event)
        }

        // Phase 3: insert the remaining events in the db and update the recycler
        return if (events.size == 0) {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            })
            true
        } else {
            act.mainViewModel.insertAll(events)
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_success))
            })
            true
        }
    }

    // Get the contacts and save them in a map
    private fun getCalendarEvents(): List<ImportedEvent> {
        val eventInfo = mutableListOf<ImportedEvent>()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID, // 0
            CalendarContract.Instances.BEGIN, // 1
            CalendarContract.Instances.TITLE, // 2
            CalendarContract.Instances.RRULE, // 3
            CalendarContract.Instances.DESCRIPTION // 4
        )

        // The indices for the projection array above
        val projectionIdIndex = 0
        val projectionBeginIndex = 1
        val projectionTitleIndex = 2
        val projectionRruleIndex = 3
        val projectionDescriptionIndex = 4

        // Specify the date range you want to search for recurring event instances
        val startMillis: Long = LocalDate.now().toEpochDay() * 24 * 60 * 60 * 1000
        val endMillis: Long = LocalDate.now().plusYears(1L).toEpochDay() * 24 * 60 * 60 * 1000

        // Construct the query with the desired date range
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        // Retrieve each part of the name and the ID
        val resolver: ContentResolver = context.contentResolver
        val cursor = resolver.query(
            builder.build(),
            projection,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            // For each event, get any possible data
            while (cursor.moveToNext()) {
                val id = cursor.getLong(projectionIdIndex)
                val title = cursor.getString(projectionTitleIndex)
                val description = cursor.getString(projectionDescriptionIndex)
                val begin = cursor.getLong(projectionBeginIndex)
                val rule = cursor.getString(projectionRruleIndex)

                Log.d("import", "Name is: $title")
                Log.d("import", "Other data: $id, $description, $begin, $rule")

                // Create a custom event if the event rule is "yearly"
                if (rule != null && rule.contains("FREQ=YEARLY")) {
                    // Don't consider any year, but the best approach would be to find the first occurrence of each event
                    val date = LocalDate.ofEpochDay(begin / (24 * 60 * 60 * 1000)).withYear(1970)
                    val importedEvent = ImportedEvent(
                        id.toString(),
                        title,
                        date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        null,
                        EventCode.OTHER.name,
                        description
                    )
                    eventInfo.add(importedEvent)
                }
            }
        }
        cursor?.close()
        return eventInfo
    }
}