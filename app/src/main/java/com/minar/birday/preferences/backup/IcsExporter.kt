package com.minar.birday.preferences.backup

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.core.content.FileProvider
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.Event
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.LocalDateTypeConverter
import com.minar.birday.utilities.EventCalendar
import com.minar.birday.utilities.alternativeCalendar
import com.minar.birday.utilities.eventsToIcs
import com.minar.birday.utilities.getStringForTypeCodename
import com.minar.birday.utilities.nextOccurrence
import com.minar.birday.utilities.resultToEvent
import java.io.File
import java.io.IOException
import java.time.LocalDate


// How many dates an event following an alternative calendar carries, a lifetime of calendars
private const val LUNAR_DATES = 20

// Where the file of a shared event is written, as declared in the file provider paths
private const val SHARED_EVENTS_FOLDER = "shared_events"

class IcsExporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.setOnClickListener(this)
    }

    // Vibrate, export every event and share the file right away
    override fun onClick(v: View) {
        val act = context as? MainActivity ?: return
        act.vibrate()
        if (act.mainViewModel.allEventsUnfiltered.value.isNullOrEmpty()) {
            act.showSnackbar(context.getString(R.string.no_events))
            return
        }
        act.saveIcs.launch("BirdayIcs_${LocalDate.now()}.ics")
    }

    companion object {
        // Every event, notes included, in the given uri. False if it couldn't be written
        fun exportEventsIcs(context: Context, uri: Uri): Boolean {
            val events = EventDatabase.getBirdayDatabase(context).eventDao()
                .getOrderedEventsStatic().map { resultToEvent(it) }
            return try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(icsOf(context, events, includeNotes = true).toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Cannot open output stream for uri: $uri")
                true
            } catch (_: Exception) {
                false
            }
        }

        // A single event in a file to share, picture included, without its notes: they're personal
        fun shareableIcs(context: Context, event: Event): Uri {
            val folder = File(context.cacheDir, SHARED_EVENTS_FOLDER).apply { mkdirs() }
            // A readable file name, the one the chat shows, without what a file system refuses
            val fileName = listOfNotNull(event.name, event.surname?.takeIf { it.isNotBlank() })
                .joinToString(" ")
                .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "")
                .ifBlank { "Birday" } + ".ics"
            val file = File(folder, fileName)
            file.writeText(
                icsOf(context, listOf(event), includeNotes = false, includeImages = true),
                Charsets.UTF_8
            )
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

        private fun icsOf(
            context: Context,
            events: List<Event>,
            includeNotes: Boolean,
            includeImages: Boolean = false,
        ): String {
            val calendarsOn = alternativeCalendar(context) != null
            return eventsToIcs(
                events,
                summary = { event ->
                    val name = listOfNotNull(event.name, event.surname?.takeIf { it.isNotBlank() })
                        .joinToString(" ")
                    "$name (${getStringForTypeCodename(context, event.type ?: "")})"
                },
                lunarDates = { event -> if (calendarsOn) lunarDates(event) else null },
                february28 = LocalDateTypeConverter.useFebruary28,
                includeNotes = includeNotes,
                includeImages = includeImages,
            )
        }

        // The next dates of an event following an alternative calendar, none for a Gregorian one
        private fun lunarDates(event: Event): List<LocalDate>? {
            val calendar = EventCalendar.fromKey(event.calendar) ?: return null
            if (event.yearMatter != true) return null
            var from = LocalDate.now()
            return List(LUNAR_DATES) {
                nextOccurrence(event.originalDate, calendar, from).also { from = it.plusDays(1) }
            }
        }
    }
}
