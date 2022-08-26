package com.minar.birday.backup

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.gson.GsonBuilder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.LocalDateJsonSerializer
import com.minar.birday.utilities.shareFile
import java.io.File
import java.time.LocalDate


@ExperimentalStdlibApi
class JsonExporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
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
        if (act.mainViewModel.nextEvents.value.isNullOrEmpty())
            act.showSnackbar(context.getString(R.string.no_events))
        else {
            val thread = Thread {
                val exported = exportEventsJson(context)
                if (exported.isNotBlank()) shareFile(context, exported)
            }
            thread.start()
        }
    }

    // Export the room database to a file in Android/data/com.minar.birday/files
    private fun exportEventsJson(context: Context): String {
        // Take the list of events
        val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        val events = eventDao.getOrderedEventsStatic()
        val sb = StringBuilder()
        val builder = GsonBuilder().registerTypeAdapter(
            LocalDate::class.java,
            LocalDateJsonSerializer().nullSafe()
        )
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create()
        sb.append(builder.toJson(events))

        val appDirectory = File(context.getExternalFilesDir(null)!!.absolutePath)
        val fileName = "BirdayJson_${LocalDate.now()}.json"
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