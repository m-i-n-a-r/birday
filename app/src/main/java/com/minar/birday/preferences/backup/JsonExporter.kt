package com.minar.birday.preferences.backup

import android.content.Context
import android.content.Intent
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
import com.minar.birday.utilities.resultToEvent
import java.io.File
import java.io.IOException
import java.time.LocalDate


class JsonExporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
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
        val fileName = "BirdayJson_${LocalDate.now()}.json"
        act.saveJson.launch(fileName)
    }

    companion object  {
        // Convert the data in a JSON file and save it in a given uri
        fun exportEventsJson(
            context: Context,
            uri: Uri?
        ): String {
            // Take the list of events
            val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            val eventResults = eventDao.getOrderedEventsStatic()

            // Transform the list in a list of simple events
            val events = mutableListOf<Event>()
            eventResults.forEach { events.add(resultToEvent(it)) }

            // Transform the entire list in a JSON string
            val builder = GsonBuilder().registerTypeAdapter(
                LocalDate::class.java,
                LocalDateJsonSerializer().nullSafe()
            )
                .excludeFieldsWithoutExposeAnnotation()
                .setPrettyPrinting()
                .create()
            val json = builder.toJson(events)
            try {
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                        os.flush()
                    } ?: throw IOException("Cannot open output stream for uri: $uri")
                    // Try to persist permission
                    try {
                        val takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (_: Exception) {
                        // Ignore if provider doesn't allow persistable permission
                    }
                    return uri.toString()
                } else {
                    // Legacy: write to app files dir
                    val appDir = File(context.getExternalFilesDir(null)!!.absolutePath)
                    val fileName = "BirdayJson_${LocalDate.now()}.json"
                    val dest = File(appDir, fileName)
                    dest.writeText(json, Charsets.UTF_8)
                    return dest.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return ""
            }
        }
    }
}