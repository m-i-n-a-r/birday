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
import com.minar.birday.utilities.COLUMN_INPUT1
import com.minar.birday.utilities.COLUMN_INPUT10
import com.minar.birday.utilities.COLUMN_INPUT2
import com.minar.birday.utilities.COLUMN_INPUT3
import com.minar.birday.utilities.COLUMN_INPUT4
import com.minar.birday.utilities.COLUMN_INPUT5
import com.minar.birday.utilities.COLUMN_INPUT6
import com.minar.birday.utilities.COLUMN_INPUT7
import com.minar.birday.utilities.COLUMN_INPUT8
import com.minar.birday.utilities.COLUMN_INPUT9
import com.minar.birday.utilities.COLUMN_NAME
import com.minar.birday.utilities.COLUMN_NOTES
import com.minar.birday.utilities.COLUMN_SURNAME
import com.minar.birday.utilities.COLUMN_TYPE
import com.minar.birday.utilities.COLUMN_VEHICLE_INSURANCEPROVIDER
import com.minar.birday.utilities.COLUMN_VEHICLE_MANUFACTURER_NAME
import com.minar.birday.utilities.COLUMN_VEHICLE_MANUFACTURER_NAME1
import com.minar.birday.utilities.COLUMN_VEHICLE_MANUFACTURER_NAME2
import com.minar.birday.utilities.COLUMN_VEHICLE_MANUFACTURER_NAME3
import com.minar.birday.utilities.COLUMN_VEHICLE_MODELNAME
import com.minar.birday.utilities.COLUMN_VEHICLE_MODELNAME1
import com.minar.birday.utilities.COLUMN_VEHICLE_MODELNAME2
import com.minar.birday.utilities.COLUMN_VEHICLE_MODELNAME3
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
        try {
            // Check if the column names are in the file, in the smartest way possible
            if (csvList[0].lowercase().contains("date")) {
                columnsMapping = smartDetectColumns(csvList[0], separator)
                if (columnsMapping.isNullOrEmpty()) {
                    separator = ';'
                    columnsMapping = smartDetectColumns(csvList[0], separator)
                }
            }
            // If the line didn't contain the columns, try parsing it
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
            } else {
                for (i in 1 until csvList.size) {
                    val rowValues = csvList[i].lowercase().split(separator)
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
                            type = rowValues.getOrNull(columnsMapping.getOrDefault(COLUMN_TYPE, -1))
                                ?: "",
                            yearMatter = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_YEAR_MATTER,
                                    -1
                                )
                            )?.toBooleanStrict() ?: true,

                            //vehicle insurance provider
                            manufacturerName = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MANUFACTURER_NAME,
                                    -1
                                )
                            ) ?: "",
                            manufacturerName1 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MANUFACTURER_NAME1,
                                    -1
                                )
                            ) ?: "",
                            manufacturerName2 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MANUFACTURER_NAME2,
                                    -1
                                )
                            ) ?: "",
                            manufacturerName3 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MANUFACTURER_NAME3,
                                    -1
                                )
                            ) ?: "",

                            modelName = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MODELNAME,
                                    -1
                                )
                            ) ?: "",
                            modelName1 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MODELNAME1,
                                    -1
                                )
                            ) ?: "",
                            modelName2 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MODELNAME2,
                                    -1
                                )
                            ) ?: "",
                            modelName3 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_MODELNAME3,
                                    -1
                                )
                            ) ?: "",
                            insuranceProvider = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_VEHICLE_INSURANCEPROVIDER,
                                    -1
                                )
                            ) ?: "",
                            notes = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_NOTES,
                                    -1
                                )
                            ) ?: "",

                            //vehicle insurance renewal add event
                            input1 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT1,
                                    -1
                                )
                            ) ?: "",
                            input2 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT2,
                                    -1
                                )
                            ) ?: "",
                            input3 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT3,
                                    -1
                                )
                            ) ?: "",
                            input4 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT4,
                                    -1
                                )
                            ) ?: "",
                            input5 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT5,
                                    -1
                                )
                            ) ?: "",
                            input6 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT6,
                                    -1
                                )
                            ) ?: "",
                            input7 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT7,
                                    -1
                                )
                            ) ?: "",
                            input8 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT8,
                                    -1
                                )
                            ) ?: "",
                            input9 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT9,
                                    -1
                                )
                            ) ?: "",
                            input10 = rowValues.getOrNull(
                                columnsMapping.getOrDefault(
                                    COLUMN_INPUT10,
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
            if (it.contains("date") && rowMapping[COLUMN_DATE] != null) {
                rowMapping[COLUMN_DATE] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("name") && rowMapping[COLUMN_NAME] != null) {
                rowMapping[COLUMN_NAME] = rowValues.indexOf(it)
                return@forEach
            }
            if ((it.contains("surname") || it.contains("last name")) && rowMapping[COLUMN_SURNAME] != null) {
                rowMapping[COLUMN_SURNAME] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("note") && rowMapping[COLUMN_NOTES] != null) {
                rowMapping[COLUMN_NOTES] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("type") && rowMapping[COLUMN_TYPE] != null) {
                rowMapping[COLUMN_TYPE] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("year") && rowMapping[COLUMN_YEAR_MATTER] != null) {
                rowMapping[COLUMN_YEAR_MATTER] = rowValues.indexOf(it)
                return@forEach
            }
            //vehicle insurance event
            if (it.contains("manufacturerName") && rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME] != null) {
                rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("manufacturerName1") && rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME1] != null) {
                rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME1] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("manufacturerName2") && rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME2] != null) {
                rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME2] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("manufacturerName3") && rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME3] != null) {
                rowMapping[COLUMN_VEHICLE_MANUFACTURER_NAME3] = rowValues.indexOf(it)
                return@forEach
            }

            if (it.contains("insuranceProvider") && rowMapping[COLUMN_VEHICLE_INSURANCEPROVIDER] != null) {
                rowMapping[COLUMN_VEHICLE_INSURANCEPROVIDER] = rowValues.indexOf(it)
                return@forEach
            }

            if (it.contains("modelName") && rowMapping[COLUMN_VEHICLE_MODELNAME] != null) {
                rowMapping[COLUMN_VEHICLE_MODELNAME] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("modelName1") && rowMapping[COLUMN_VEHICLE_MODELNAME1] != null) {
                rowMapping[COLUMN_VEHICLE_MODELNAME1] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("modelName2") && rowMapping[COLUMN_VEHICLE_MODELNAME2] != null) {
                rowMapping[COLUMN_VEHICLE_MODELNAME2] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("modelName3") && rowMapping[COLUMN_VEHICLE_MODELNAME3] != null) {
                rowMapping[COLUMN_VEHICLE_MODELNAME3] = rowValues.indexOf(it)
                return@forEach
            }

            //vehicle insurance renewal add event
            if (it.contains("input1") && rowMapping[COLUMN_INPUT1] != null) {
                rowMapping[COLUMN_INPUT1] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input2") && rowMapping[COLUMN_INPUT2] != null) {
                rowMapping[COLUMN_INPUT2] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input3") && rowMapping[COLUMN_INPUT3] != null) {
                rowMapping[COLUMN_INPUT3] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input4") && rowMapping[COLUMN_INPUT4] != null) {
                rowMapping[COLUMN_INPUT4] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input5") && rowMapping[COLUMN_INPUT5] != null) {
                rowMapping[COLUMN_INPUT5] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input6") && rowMapping[COLUMN_INPUT6] != null) {
                rowMapping[COLUMN_INPUT6] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input7") && rowMapping[COLUMN_INPUT7] != null) {
                rowMapping[COLUMN_INPUT7] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input8") && rowMapping[COLUMN_INPUT8] != null) {
                rowMapping[COLUMN_INPUT8] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input9") && rowMapping[COLUMN_INPUT9] != null) {
                rowMapping[COLUMN_INPUT9] = rowValues.indexOf(it)
                return@forEach
            }
            if (it.contains("input10") && rowMapping[COLUMN_INPUT10] != null) {
                rowMapping[COLUMN_INPUT10] = rowValues.indexOf(it)
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

        // Map columns explicitly to variables
        val name = rowValues.getOrNull(1)?.trim() ?: ""
        val surname = rowValues.getOrNull(2)?.trim() ?: ""
        val date = try {
            rowValues.getOrNull(0)?.trim()?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
            null
        }
        val notes = rowValues.getOrNull(14)?.trim() ?: ""
        val type = rowValues.getOrNull(3)?.trim() ?: EventCode.BIRTHDAY.name
        val yearMatter = rowValues.getOrNull(4)?.toBooleanStrictOrNull() ?: true

        //vehicle insurance due date add event
        val manufacturerName = rowValues.getOrNull(5)?.trim() ?: ""
        val manufacturerName1 = rowValues.getOrNull(6)?.trim() ?: ""
        val manufacturerName2 = rowValues.getOrNull(7)?.trim() ?: ""
        val manufacturerName3 = rowValues.getOrNull(8)?.trim() ?: ""
        val insuranceProvider = rowValues.getOrNull(13)?.trim() ?: ""
        val modelName = rowValues.getOrNull(9)?.trim() ?: ""
        val modelName1 = rowValues.getOrNull(10)?.trim() ?: ""
        val modelName2 = rowValues.getOrNull(11)?.trim() ?: ""
        val modelName3 = rowValues.getOrNull(12)?.trim() ?: ""

        //vehicle insurance renewal add event
        val input1 = rowValues.getOrNull(15)?.trim() ?: ""
        val input2 = rowValues.getOrNull(16)?.trim() ?: ""
        val input3 = rowValues.getOrNull(17)?.trim() ?: ""
        val input4 = rowValues.getOrNull(18)?.trim() ?: ""
        val input5 = rowValues.getOrNull(19)?.trim() ?: ""
        val input6 = rowValues.getOrNull(20)?.trim() ?: ""
        val input7 = rowValues.getOrNull(21)?.trim() ?: ""
        val input8 = rowValues.getOrNull(22)?.trim() ?: ""
        val input9 = rowValues.getOrNull(23)?.trim() ?: ""
        val input10 = rowValues.getOrNull(24)?.trim() ?: ""

        // Create and return the event if mandatory fields exist
        return if (date != null && name.isNotBlank())
            normalizeEvent(
                Event(
                    id = 0,
                    name = name,
                    surname = surname,
                    type = type,
                    originalDate = date,
                    yearMatter = yearMatter,
                    notes = notes,
                    //vehicle insurance due date add event
                    manufacturerName = manufacturerName,
                    manufacturerName1 = manufacturerName1,
                    manufacturerName2 = manufacturerName2,
                    manufacturerName3 = manufacturerName3,
                    insuranceProvider = insuranceProvider,
                    modelName = modelName,
                    modelName1 = modelName1,
                    modelName2 = modelName2,
                    modelName3 = modelName3,
                    //vehicle insurance renewal add event
                    input1 = input1,
                    input2 = input2,
                    input3 = input3,
                    input4 = input4,
                    input5 = input5,
                    input6 = input6,
                    input7 = input7,
                    input8 = input8,
                    input9 = input9,
                    input10 = input10
                )
            )
        else null
    }

}