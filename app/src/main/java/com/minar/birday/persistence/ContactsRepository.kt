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
import com.minar.birday.model.EventCode
import com.minar.birday.model.ImportedEvent
import com.minar.birday.utilities.bitmapToByteArray
import com.minar.birday.utilities.getBitmapSquareSize

/**
 * Fetches the contacts from the system.
 *
 * It needs all contacts related permissions in order to work properly.
 */
class ContactsRepository {
    /**
     * Read all the possible events from the contacts.
     */
    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    fun getContactEvents(resolver: ContentResolver): List<ImportedEvent> {
        val contactInfo = mutableListOf<ImportedEvent>()

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
            ContactsContract.Data.MIMETYPE + " = ? AND " +
                    ContactsContract.Data.IN_VISIBLE_GROUP + " = ?",
            arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, "1"),
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
                if (contactInfo.any { it.id == id }) continue

                val prefix = cursor.getStringOrNull(prefixValue) ?: ""
                val firstName = cursor.getString(firstNameValue)
                val middleName = cursor.getStringOrNull(middleNameValue) ?: ""
                val lastName = cursor.getStringOrNull(lastNameValue) ?: ""
                val suffix = cursor.getStringOrNull(suffixValue) ?: ""

                // The format at this time is first name, last name (+ extra stuff)
                val birdayFirstName = "$prefix $firstName $middleName".replace(',', ' ').trim()
                val birdayLastName = "$lastName $suffix".replace(',', ' ').trim()
                val birdayName =
                    if (birdayLastName.isBlank())
                        birdayFirstName
                    else
                        "$birdayFirstName,$birdayLastName".replace("\\s".toRegex(), " ")
                Log.d("import", "birday name is: $birdayName for id $id")

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

                // Retrieve the events for the current contact
                val eventCursor = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Event.DATA,
                        ContactsContract.CommonDataKinds.Event.TYPE,
                        ContactsContract.CommonDataKinds.Event.LABEL
                    ),
                    ContactsContract.Data.CONTACT_ID + " = " + id + " AND " +
                            ContactsContract.Data.MIMETYPE + " = '" +
                            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE + "'",
                    null,
                    null
                )

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
                        val importedEvent = if (eventCustomLabel != null) ImportedEvent(
                            id,
                            birdayName,
                            birthday,
                            image,
                            eventType.name,
                            eventCustomLabel
                        ) else ImportedEvent(id, birdayName, birthday, image, eventType.name)
                        contactInfo.add(importedEvent)
                    }
                    eventCursor.close()
                }
            }
        }
        cursor?.close()
        return contactInfo
    }
}