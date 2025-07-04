package com.minar.birday.preferences.backup

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.utilities.COLUMN_DATE
import com.minar.birday.utilities.COLUMN_NAME
import com.minar.birday.utilities.COLUMN_NOTES
import com.minar.birday.utilities.COLUMN_SURNAME
import com.minar.birday.utilities.COLUMN_TYPE
import com.minar.birday.utilities.COLUMN_YEAR_MATTER
import com.minar.birday.utilities.normalizeEvent
import java.time.LocalDate


class CsvImporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {
    private val act = context as MainActivity

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    // Vibrate and import the backup if possible
    override fun onClick(v: View) {
        act.vibrate()
        act.selectBackup.launch("text/comma-separated-values")
    }

    // Import a backup overwriting any existing data and checking if the file is valid
    fun importEventsCsv(context: Context, fileUri: Uri): Boolean {
        // Read the file as a list of rows
        var separator = ','
        val fileStream = context.contentResolver.openInputStream(fileUri)!!
        val csvString = fileStream.bufferedReader().use { it.readText() }
        val csvList = csvString.split('\n')
        val eventList = mutableListOf<Event>()
        var columnsMapping: Map<String, Int>? = null
        // Encapsulate in a try, to avoid crashes
        try {
            // Check if the column names are in the file, in the smartest way possible
            val headerLower = csvList[0].lowercase()
            if (headerLower.contains("date")) {
                columnsMapping = smartDetectColumns(headerLower, separator)
                // Check if a mapping was found using the comma separator
                if (columnsMapping.isNullOrEmpty()) {
                    separator = ';'
                    // Try again using the semicolon
                    columnsMapping = smartDetectColumns(headerLower, separator)
                }
            }
            // If the line didn't contain the column names, try parsing it anyway
            if (columnsMapping.isNullOrEmpty()) {
                // Trying again with the comma if something was found with the ';' is trivial
                separator = ','
                var detectedEvent: Event? = smartDetectEvent(csvList[0], separator)
                if (detectedEvent != null) eventList.add(detectedEvent)
                else {
                    // At this point, if something is detected using this separator, that's the one
                    separator = ';'
                    detectedEvent = smartDetectEvent(csvList[0], separator)
                    if (detectedEvent != null) eventList.add(detectedEvent)
                }
            }
            // Depending on the first element, build the others
            if (columnsMapping.isNullOrEmpty()) {
                for (i in 1 until csvList.size) {
                    // Create an entity for each valid line
                    val detectedEvent: Event? = smartDetectEvent(csvList[i], separator)
                    if (detectedEvent != null) eventList.add(detectedEvent)
                }
            }
            // We have a mapping and we can add events securely
            else {
                for (i in 1 until csvList.size) {
                    val rowValues = csvList[i].split(separator)
                    // Detect if the type is valid first
                    val rowItem = rowValues.getOrNull(columnsMapping.getOrDefault(COLUMN_TYPE, -1))
                        ?.uppercase()
                        ?: EventCode.BIRTHDAY.name
                    val type = if (EventCode.entries.map { it.name }.contains(rowItem)) rowItem
                    else EventCode.BIRTHDAY.name
                    try {
                        // Depending on the detected columns, create the event objects
                        val event = Event(
                            id = 0,
                            originalDate = LocalDate.parse(rowValues[columnsMapping[COLUMN_DATE]!!]),
                            name = rowValues[columnsMapping[COLUMN_NAME]!!],
                            surname = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_SURNAME,
                                    -1
                                )
                            ) ?: "",
                            type = type,
                            yearMatter = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_YEAR_MATTER,
                                    -1
                                )
                            )?.toBooleanStrict() ?: true,
                            notes = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_NOTES,
                                    -1
                                )
                            ) ?: ""
                        )
                        eventList.add(normalizeEvent(event))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            // If the list is empty, the file was probably malformed
            if (eventList.isEmpty())
                (context as MainActivity)
                    .showSnackbar(context.getString(R.string.import_nothing_found))
            // Bulk insert, using the standard duplicate detection strategy
            act.mainViewModel.insertAll(eventList)
            // Done. There's no need to restart the app
            fileStream.close()
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_success))
        } catch (e: Exception) {
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_failure))
            e.printStackTrace()
            return false
        }
        return true
    }

    // Return a map containing the order of the columns from the column names row
    private fun smartDetectColumns(csvRow: String, delimiter: Char = ','): Map<String, Int>? {
        val rowValues = csvRow.lowercase().split(delimiter)
        val rowMapping = mutableMapOf<String, Int>()
        // Search each field: name and date (mandatory), type, surname, yearMatter, notes
        rowValues.forEach {
            val column = it.trim()
            // Each field of the mapping can be assigned once
            if (column.contains("date") && rowMapping[COLUMN_DATE] == null) {
                rowMapping[COLUMN_DATE] = rowValues.indexOf(it)
                return@forEach
            }
            if (column == "name" || (column.contains("name") &&
                        column.contains("first")) &&
                rowMapping[COLUMN_NAME] == null
            ) {
                rowMapping[COLUMN_NAME] = rowValues.indexOf(it)
                return@forEach
            }
            if ((column == ("surname") || column.contains("last")) && rowMapping[COLUMN_SURNAME] == null) {
                rowMapping[COLUMN_SURNAME] = rowValues.indexOf(it)
                return@forEach
            }
            if (column.contains("note") && rowMapping[COLUMN_NOTES] == null) {
                rowMapping[COLUMN_NOTES] = rowValues.indexOf(it)
                return@forEach
            }
            if (column.contains("type") && rowMapping[COLUMN_TYPE] == null) {
                rowMapping[COLUMN_TYPE] = rowValues.indexOf(it)
                return@forEach
            }
            if (column.contains("year") && rowMapping[COLUMN_YEAR_MATTER] == null) {
                rowMapping[COLUMN_YEAR_MATTER] = rowValues.indexOf(it)
                return@forEach
            }
        }
        return if (rowMapping[COLUMN_DATE] != null && rowMapping[COLUMN_NAME] != null)
            rowMapping
        else null
    }

    // Return an event starting from a data line, if possible
    private fun smartDetectEvent(csvRow: String, delimiter: Char = ','): Event? {
        val rowValues = csvRow.split(delimiter)
        var date: LocalDate? = null
        var name = ""
        var surname = ""
        var notes = ""
        var type: String = EventCode.BIRTHDAY.name
        var yearMatter = true

        // First, search for the only two mandatory columns, name and date
        rowValues.forEach { rowItem ->
            // Date detection
            if (date == null)
                try {
                    date = LocalDate.parse(rowItem)
                    return@forEach // Acts like continue, i.e. skip to the next iteration
                } catch (_: Exception) {
                }
            // Boolean (yearMatter) detection
            if (rowItem.toBooleanStrictOrNull() != null) {
                yearMatter = rowItem.toBooleanStrict()
                return@forEach
            }
            // Type detection
            if (EventCode.entries.map { it.name }.contains(rowItem)) {
                type = rowItem
                return@forEach
            }
            // Name. Assuming that name comes first, surname second
            if (name.isBlank() && rowItem.length < 30) {
                name = rowItem
                return@forEach
            }
            // Surname. Assuming that it comes second (since it's not mandatory)
            if (surname.isBlank() && rowItem.length < 30) {
                surname = rowItem
                return@forEach
            }
            // Notes detection
            if (rowItem.length > 30 || (name.isNotBlank() && surname.isNotBlank()))
                notes = rowItem
        }

        // Create and return the event (normalizing it) if each necessary field exists
        return if (date != null && name.isNotBlank())
            normalizeEvent(
                Event(
                    id = 0,
                    name = name,
                    surname = surname,
                    type = type,
                    originalDate = date!!,
                    yearMatter = yearMatter,
                    notes = notes
                )
            )
        else null
    }

}