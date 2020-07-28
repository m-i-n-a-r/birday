package com.minar.birday.backup

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.facebook.shimmer.ShimmerFrameLayout
import com.minar.birday.activities.MainActivity
import com.minar.birday.R
import com.minar.birday.model.Event
import java.time.LocalDate
import kotlin.concurrent.thread

class ContactsImporter(context: Context?, attrs: AttributeSet?) : Preference(context, attrs), View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        val shimmer = v as ShimmerFrameLayout

        // Disable the onclick and show the shimmer if the option is enabled
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val shimmerEnabled = sp.getBoolean("shimmer", false)
        v.setOnClickListener(null)
        if(shimmerEnabled) {
            shimmer.startShimmer()
            shimmer.showShimmer(true)
        }
        act.vibrate()
        thread {
            importContacts(context)
            (context as MainActivity).runOnUiThread {
                if(shimmerEnabled) {
                    shimmer.stopShimmer()
                    shimmer.hideShimmer()
                }
                v.setOnClickListener(this)
            }
        }
    }

    // Import the contacts from Google Contacts
    fun importContacts(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for contacts permission
        val permission = act.askContactsPermission(102)
        if (!permission) return false

        // Phase 1: get every contact having at least a name and a birthday
        val contacts = getContacts()

        // Phase 2: convert the extracted data in an Event List, verify duplicates
        val events = mutableListOf<Event>()
        loop@ for (contact in contacts) {
            // Take the name and split it to separate name and surname
            val splitterName = contact.value[0].split(",")
            var name: String
            var surname = ""
            var date: LocalDate
            when (splitterName.size) {
                // Not considering surname only contacts, but considering name only
                1 -> name = splitterName[0].trim()
                2 -> {
                    name = splitterName[1].trim()
                    surname = splitterName[0].trim()
                }
                else -> continue@loop
            }

            try {
                // Missing year, put 2000 as a placeholder
                var parseDate = contact.value[1]
                if (contact.value[1].length < 8) parseDate = contact.value[1].replaceFirst("-", "2000")
                date = LocalDate.parse(parseDate)
            }
            catch (e: Exception) { continue }
            val event = Event(
                id = 0,
                name = name,
                surname = surname,
                originalDate = date
            )
            events.add(event)
        }

        // Phase 3: insert the remaining events in the db and update the recycler
        return if (events.size == 0) {
            context.runOnUiThread(Runnable {
                Toast.makeText(context, context.getString(R.string.import_nothing_found), Toast.LENGTH_SHORT).show()
            })
            true
        } else {
            events.forEach { act.homeViewModel.insert(it) }
            context.runOnUiThread(Runnable {
                    Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                })
            true
        }
    }

    // Get the contacts and save them in a map
    private fun getContacts(): Map<String, List<String>> {
        val nameBirth = mutableMapOf<String, List<String>>()

        // Retrieve name and id
        val resolver: ContentResolver = context.contentResolver
        val cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        if (cursor != null) {
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE))
                    // Retrieve the birthday
                    val bd = context.contentResolver
                    val bdc: Cursor? = bd.query(
                        ContactsContract.Data.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Event.DATA),
                        ContactsContract.Data.CONTACT_ID + " = " + id + " AND " + ContactsContract.Data.MIMETYPE + " = '" +
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE + "' AND " + ContactsContract.CommonDataKinds.Event.TYPE +
                                " = " + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY, null, ContactsContract.Data.DISPLAY_NAME
                    )

                    if (bdc != null) {
                        if (bdc.count > 0) {
                            while (bdc.moveToNext()) {
                                // Using a list as key will prevent collisions on same name
                                val birthday: String = bdc.getString(0)
                                val person = listOf<String>(name, birthday)
                                nameBirth[id] = person
                            }
                        }
                        bdc.close()
                    }
                }
            }
        }
        cursor?.close()
        return nameBirth
    }

}