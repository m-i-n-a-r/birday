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
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.LocalDateJsonSerializer
import com.minar.birday.utilities.normalizeEvent
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.thread


@ExperimentalStdlibApi
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
        val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        val gsonBuilder = GsonBuilder().registerTypeAdapter(
            LocalDate::class.java,
            LocalDateJsonSerializer().nullSafe()
        ).create()
        try {
            val importedEvents = gsonBuilder.fromJson(jsonString, Array<Event>::class.java).toList()
            // Normalize the events and add them to another list
            val normalizedEvents = mutableListOf<Event>()
            importedEvents.forEach { normalizedEvents.add(normalizeEvent(it)) }

            // Bulk insert, using the standard duplicate detection strategy
            thread {
                eventDao.insertAllEvent(normalizedEvents)
            }
            fileStream.close()
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_success))
        } catch (e: Exception) {
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_failure))
            e.printStackTrace()
            return false
        }
        // No restart needed, since the events are normally inserted in the DB
        return true
    }

}