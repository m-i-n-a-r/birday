package com.minar.birday.backup

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.ContactsContract
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.facebook.shimmer.ShimmerFrameLayout
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.Event
import com.minar.birday.model.ImportedContact
import com.minar.birday.utilities.bitmapToByteArray
import com.minar.birday.utilities.getBitmapSquareSize
import java.time.LocalDate
import kotlin.concurrent.thread


@ExperimentalStdlibApi
class ContactsImporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {

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
    fun importContacts(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for contacts permission
        val permission = act.askContactsPermission(102)
        if (!permission) return false

        // Phase 1: get every contact having at least a name and a birthday
        val contacts = getContacts()
        if (contacts.isEmpty()) return true

        // Phase 2: convert the extracted data in an Event List, verify duplicates
        val events = mutableListOf<Event>()
        loop@ for (contact in contacts) {
            // Take the name and split it to separate name and surname
            val splitName = contact.completeName.split(",")
            var name: String
            var surname = ""
            var date: LocalDate
            var countYear = true
            when (splitName.size) {
                // Not considering surname only contacts, but considering name only
                1 -> name = splitName[0].trim()
                2 -> {
                    name = splitName[1].trim()
                    surname = splitName[0].trim()
                }
                else -> continue@loop
            }

            try {
                // Missing year, simply don't consider the year exactly like the contacts app does
                var parseDate = contact.birthday
                if (parseDate.length < 8) {
                    parseDate = contact.birthday.replaceFirst("-", "1970")
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
                image = contact.image,
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
    private fun getContacts(): List<ImportedContact> {
        val contactInfo = mutableListOf<ImportedContact>()

        // Retrieve name and id
        val resolver: ContentResolver = context.contentResolver
        val cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        if (cursor != null) {
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val idValue = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameValue =
                        cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE)
                    // Control the values, even if they should always exist
                    if (idValue < 0 || nameValue < 0) continue

                    val id = cursor.getString(idValue)
                    val name = cursor.getString(nameValue)
                    // Get the image, if any, and convert it to byte array
                    val imageStream = ContactsContract.Contacts.openContactPhotoInputStream(
                        resolver,
                        ContentUris.withAppendedId(
                            ContactsContract.Contacts.CONTENT_URI,
                            id.toLong()
                        )
                    )
                    val bitmap = BitmapFactory.decodeStream(imageStream)
                    var image: ByteArray? = null
                    if (bitmap != null) {
                        // Check if the image is too big and resize it to a square if needed
                        var dimension = getBitmapSquareSize(bitmap)
                        if (dimension > 450) dimension = 450
                        val resizedBitmap = ThumbnailUtils.extractThumbnail(
                            bitmap,
                            dimension,
                            dimension,
                            ThumbnailUtils.OPTIONS_RECYCLE_INPUT,
                        )
                        image = bitmapToByteArray(resizedBitmap)
                    }
                    // Retrieve the birthday
                    val bd = context.contentResolver
                    val bdc: Cursor? = bd.query(
                        ContactsContract.Data.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Event.DATA),
                        ContactsContract.Data.CONTACT_ID + " = " + id + " AND " + ContactsContract.Data.MIMETYPE + " = '" +
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE + "' AND " + ContactsContract.CommonDataKinds.Event.TYPE +
                                " = " + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                        null,
                        ContactsContract.Data.DISPLAY_NAME
                    )

                    if (bdc != null && bdc.count > 0) {
                        while (bdc.moveToNext()) {
                            // Using an object model to store the information
                            val birthday: String = bdc.getString(0)
                            val importedContact = ImportedContact(id, name, birthday, image)
                            contactInfo.add(importedContact)
                        }
                        bdc.close()
                    }
                }
            }
        }
        cursor?.close()
        return contactInfo
    }

}