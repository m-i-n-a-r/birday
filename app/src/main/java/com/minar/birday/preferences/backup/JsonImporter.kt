package com.minar.birday.preferences.backup

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.gson.GsonBuilder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.Event
import com.minar.birday.utilities.LocalDateJsonSerializer
import com.minar.birday.utilities.normalizeEvent
import java.time.LocalDate


class JsonImporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
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
        act.selectBackup.launch("application/json")
    }

    // Import a backup with basic checks and add the entries to the current DB
    fun importEventsJson(context: Context, fileUri: Uri): Boolean {
        val fileStream = context.contentResolver.openInputStream(fileUri)!!
        val jsonString = fileStream.bufferedReader().use { it.readText() }
        val gsonBuilder = GsonBuilder().registerTypeAdapter(
            LocalDate::class.java,
            LocalDateJsonSerializer().nullSafe()
        ).create()
        try {
            val importedEvents = gsonBuilder.fromJson(jsonString, Array<Event>::class.java).toList()
            // Normalize the events and add them to another list
            val normalizedEvents = mutableListOf<Event>()
            importedEvents.forEach { normalizedEvents.add(normalizeEvent(it)) }

            // Show dialog to select what to import
            fileStream.close()
            if (normalizedEvents.isEmpty())
                (context as MainActivity).showSnackbar(context.getString(R.string.import_nothing_found))
            else
            // Show the dialog to select the events to import
                act.showImportDialog(
                    normalizedEvents,
                    title = act.getString(R.string.import_json_title)
                )
        } catch (e: Exception) {
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_failure))
            e.printStackTrace()
            return false
        }
        // No restart needed, since the events are normally inserted in the DB
        return true
    }

}