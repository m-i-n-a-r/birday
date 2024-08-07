package com.minar.birday.preferences.backup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.shareFile
import java.io.File
import java.time.LocalDate


class CsvExporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    // Vibrate, export a backup and immediately share it if possible
    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()
        // Only export if there's at least one event
        if (act.mainViewModel.allEventsUnfiltered.value.isNullOrEmpty()) {
            act.showSnackbar(context.getString(R.string.no_events))
        } else {
            val thread = Thread {
                val exported = exportEventsCsv(context)
                if (exported.isNotBlank()) shareFile(context, exported)
            }
            thread.start()
        }
    }

    // Convert the data in a CSV file and save it in Android/data/com.minar.birday/files
    private fun exportEventsCsv(context: Context): String {
        // Take the list of events
        val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        val events = eventDao.getOrderedEventsStatic()
        val sb = StringBuilder()
        // Prepare the first row, for the column names
        sb.append("date,name,surname,type,yearMatter,manufacturer_name,manufacturer_name1," +
                "manufacturer_name2,manufacturer_name3,model_name,model_name1,model_name2," +
                "model_name3,insurance_provider,notes,input1,input2,input3,input4,input5," +
                "input6,input7,input8,input9,input10\n")
        for (event in events) {
            sb.append(
                "${event.originalDate}," +
                        "${event.name.replace(',', ' ')}," +
                        "${(event.surname ?: "").replace(',', ' ')}," +
                        "${event.type}," +
                        "${event.yearMatter}," +
                        "${event.manufacturer_name}," +
                        "${event.manufacturer_name1}," +
                        "${event.manufacturer_name2}," +
                        "${event.manufacturer_name3}," +
                        "${event.model_name}," +
                        "${event.model_name1}," +
                        "${event.model_name2}," +
                        "${event.model_name3}," +
                        "${event.insurance_provider}," +
                        "${(event.notes ?: "").replace(',', ' ')},"+
                        "${event.input1}," +
                        "${event.input2}," +
                        "${event.input3}," +
                        "${event.input4}," +
                        "${event.input5}," +
                        "${event.input6}," +
                        "${event.input7}," +
                        "${event.input8}," +
                        "${event.input9}," +
                        "${event.input10}\n"

            )
        }

        val appDirectory = File(context.getExternalFilesDir(null)!!.absolutePath)
        val fileName = "BirdayCsv_${LocalDate.now()}.csv"
        val fileFullPath: String = appDirectory.path + File.separator + fileName
        // Snackbar need the UI thread to work, so they must be forced on that thread
        try {
            File(fileFullPath).writeText(sb.toString())
            (context as MainActivity).runOnUiThread { context.showSnackbar(context.getString(R.string.birday_export_success)) }
        } catch (e: Exception) {
            (context as MainActivity).runOnUiThread {
                context.showSnackbar(context.getString(R.string.birday_export_failure))
            }
            e.printStackTrace()
            return ""
        }
        return fileFullPath
    }
}