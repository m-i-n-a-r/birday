package com.minar.birday.persistence

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.database.getStringOrNull
import com.minar.birday.model.ContactInfo
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.model.ImportedEvent
import com.minar.birday.utilities.bitmapToByteArray
import com.minar.birday.utilities.getBitmapSquareSize
import java.time.LocalDate


// Fetches the contacts from the system. It needs all contacts related permissions in order to work properly
class ContactsRepository {

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    private fun getContactEvents(resolver: ContentResolver): List<ImportedEvent> {
        return queryContacts(resolver).asSequence()
            .flatMap { getEventsForContact(it, resolver) }
            .toList()
    }

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    fun queryContacts(resolver: ContentResolver): List<ContactInfo> {
        val contactsInfo = mutableListOf<ContactInfo>()
        val idsSet = mutableSetOf<String>() // For faster lookup, keep them in sync

        // Retrieve each part of the name and the ID
        val cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredName.PREFIX,
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                ContactsContract.CommonDataKinds.StructuredName.SUFFIX
            ),
            ContactsContract.Data.MIMETYPE + " = ?",
            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
            null
        )

        if (cursor != null && cursor.count > 0) {
            // For each contact, get the image and the data
            while (cursor.moveToNext()) {
                val idValue =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID)
                val prefixValue =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.PREFIX)
                val firstNameValue =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)
                val middleNameValue =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)
                val lastNameValue =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)
                val suffixValue =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.SUFFIX)
                // Control the values, the contact must have at least a name to be imported
                if (idValue < 0 || firstNameValue < 0) continue

                // Given the column indexes, retrieve the values. Don't process duplicate ids
                val id = cursor.getString(idValue)
                if (idsSet.contains(id)) continue

                val unknownString = "??"
                val prefix = cursor.getStringOrNull(prefixValue) ?: ""
                val firstName = cursor.getString(firstNameValue) ?: unknownString
                val middleName = cursor.getStringOrNull(middleNameValue) ?: ""
                val lastName = cursor.getStringOrNull(lastNameValue) ?: ""
                val suffix = cursor.getStringOrNull(suffixValue) ?: ""

                // The format at this time is first name, last name (+ extra stuff)
                val birdayFirstName = "$prefix $firstName $middleName".replace(',', ' ').trim()
                val birdayLastName = "$lastName $suffix".replace(',', ' ').trim()

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

                val contactInfo = ContactInfo(
                    id = id,
                    name = birdayFirstName,
                    surname = birdayLastName,
                    image = image,
                )
                contactsInfo.add(contactInfo)
                idsSet.add(id) // For faster lookup

                Log.d("import", "birday name is: ${contactInfo.fullName} for id $id")
            }
        }

        cursor?.close()
        return contactsInfo
    }

    private fun getEventsForContact(
        contactInfo: ContactInfo,
        resolver: ContentResolver
    ): List<ImportedEvent> {
        // Retrieve the events for the current contact
        val eventCursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Event.DATA,
                ContactsContract.CommonDataKinds.Event.TYPE,
                ContactsContract.CommonDataKinds.Event.LABEL
            ),
            ContactsContract.Data.CONTACT_ID + " = " + contactInfo.id + " AND " +
                    ContactsContract.Data.MIMETYPE + " = '" +
                    ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE + "'",
            null,
            null
        )

        val events = mutableListOf<ImportedEvent>()

        if (eventCursor != null && eventCursor.count > 0) {
            while (eventCursor.moveToNext()) {
                // Using an object model to store the information:
                // 0 is custom event type, 1 is anniversary, 2 is other, 3 is birthday
                val birthday: String = eventCursor.getString(0)
                val eventTypeNumber = eventCursor.getInt(1)
                lateinit var eventType: EventCode
                var eventCustomLabel: String? = null
                when (eventTypeNumber) {
                    0 -> {
                        eventType = EventCode.OTHER
                        eventCustomLabel = eventCursor.getString(2)
                    }
                    1 -> eventType = EventCode.ANNIVERSARY
                    2 -> eventType = EventCode.OTHER
                    else -> eventType = EventCode.BIRTHDAY
                }

                events += ImportedEvent(
                    contactInfo.id,
                    contactInfo.fullName,
                    birthday,
                    contactInfo.image,
                    eventType.name,
                    eventCustomLabel
                )
            }
        }

        eventCursor?.close()

        return events
    }

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    fun getEventsFromContacts(resolver: ContentResolver): List<Event> {
        return getContactEvents(resolver).mapNotNull { contact ->
            // Take the name and split it to separate name and surname
            val splitName = contact.completeName.split(",")
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

                Event(
                    id = 0,
                    name = name,
                    surname = surname,
                    originalDate = LocalDate.parse(parseDate),
                    yearMatter = countYear,
                    type = contact.eventType,
                    image = contact.image,
                    notes = notes
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    fun findContactIdByName(resolver: ContentResolver, givenName: String, familyName: String): String? {
        val selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME + " = ? AND " +
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME + " = ?"
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
            givenName,
            familyName
        )
        val cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID),
            selection,
            selectionArgs,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID)
                if (idIndex >= 0) return it.getString(idIndex)
            }
        }
        return null
    }

}