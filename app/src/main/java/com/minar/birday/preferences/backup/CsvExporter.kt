package com.minar.birday.preferences.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.EventDatabase
import java.io.File
import java.io.IOException
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
        val act = context as? MainActivity ?: return
        act.vibrate()
        if (act.mainViewModel.allEventsUnfiltered.value.isNullOrEmpty()) {
            act.showSnackbar(context.getString(R.string.no_events))
            return
        }
        val fileName = "BirdayCsv_${LocalDate.now()}.csv"
        act.saveCsv.launch(fileName)
    }

    companion object {
        // Convert the data in a CSV file and save it in a given uri
        fun exportEventsCsv(
            context: Context,
            uri: Uri?,
        ): String {
            val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            val sb = StringBuilder()
            val events = eventDao.getOrderedEventsStatic()
            // Prepare the first row, for the column names, and the csv itself
            sb.append("type, name, surname, yearMatter, date, notes\n")
            for (event in events) {
                sb.append(
                    "${event.type}," +
                            "${event.name.replace(',', ' ')}," +
                            "${(event.surname ?: "").replace(',', ' ')}," +
                            "${event.yearMatter}," +
                            "${event.originalDate}," +
                            "${(event.notes ?: "").replace(',', ' ')}\n"
                )
            }
            try {
                if (uri != null) {
                    // Write to SAF uri
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(sb.toString().toByteArray(Charsets.UTF_8))
                        os.flush()
                    } ?: throw IOException("Cannot open output stream for uri: $uri")
                    // Try to persist permission
                    try {
                        val takeFlags =
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (_: Exception) {
                        // Ignore if provider doesn't allow persistable permission
                    }
                    return uri.toString()
                } else {
                    // Legacy: write to app files dir
                    val appDir = File(context.getExternalFilesDir(null)!!.absolutePath)
                    val fileName = "BirdayCsv_${LocalDate.now()}.csv"
                    val dest = File(appDir, fileName)
                    dest.writeText(sb.toString(), Charsets.UTF_8)
                    return dest.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }
    }
}