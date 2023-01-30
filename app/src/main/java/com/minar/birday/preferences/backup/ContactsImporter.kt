package com.minar.birday.preferences.backup

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.facebook.shimmer.ShimmerFrameLayout
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.Event
import com.minar.birday.persistence.ContactsRepository
import java.time.LocalDate
import kotlin.concurrent.thread


@ExperimentalStdlibApi
class ContactsImporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {
    private val contactsRepository = ContactsRepository()

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        val shimmer = v as ShimmerFrameLayout

        // Disable the onclick and show the shimmer if the option is enabled
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        v.setOnClickListener(null)
        if (shimmerEnabled) {
            shimmer.startShimmer()
            shimmer.showShimmer(true)
        }
        act.vibrate()
        thread {
            importContacts(context)
            (context as MainActivity).runOnUiThread {
                if (shimmerEnabled) {
                    shimmer.stopShimmer()
                    shimmer.hideShimmer()
                }
                v.setOnClickListener(this)
            }
        }
    }

    // Import the contacts from device contacts (not necessarily Google)
    @SuppressLint("MissingPermission")
    fun importContacts(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for contacts permission
        val permission = act.askContactsPermission(102)
        if (!permission) return false

        // Phase 1: get every contact having at least a name and an event
        val contacts = contactsRepository.getContactEvents(context.contentResolver)
        if (contacts.isEmpty()) {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            })
            return true
        }

        // Phase 2: convert the extracted data in an Event List, verify duplicates
        val events = mutableListOf<Event>()
        loop@ for (contact in contacts) {
            // Take the name and split it to separate name and surname
            val splitName = contact.completeName.split(",")
            var date: LocalDate
            var countYear = true
            val notes = contact.customLabel

            val name: String = splitName[0].trim()
            val surname = if (splitName.size == 2) splitName[1].trim() else ""

            try {
                // Missing year, simply don't consider the year exactly like the contacts app does
                var parseDate = contact.eventDate
                if (parseDate.length < 8) {
                    parseDate = contact.eventDate.replaceFirst("-", "1970")
                    countYear = false
                }
                date = LocalDate.parse(parseDate)
            } catch (e: Exception) {
                continue
            }
            val event = Event(
                id = 0,
                name = name,
                surname = surname,
                originalDate = date,
                yearMatter = countYear,
                type = contact.eventType,
                image = contact.image,
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
}