package com.minar.birday.preferences.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.sqlite.db.SimpleSQLiteQuery
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.EventDatabase
import java.io.File
import java.io.IOException
import java.time.LocalDate

class BirdayExporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    // Vibrate, request SAF target, then export and share
    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()
        if (act.mainViewModel.allEventsUnfiltered.value.isNullOrEmpty()) {
            act.showSnackbar(context.getString(R.string.no_events))
            return
        }
        val fileName = "BirdayBackup_${LocalDate.now()}"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        act.saveBackup.launch(intent)
    }

    companion object {
        // Export to internal app folder and return file path
        fun exportEvents(
            context: Context,
            uri: Uri?,
            autoBackup: Boolean = false
        ): String {
            val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            // Checkpoint WAL to flush DB to disk
            eventDao.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
            val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
            // Quick sanity checks
            if (!dbFile.exists() || dbFile.length() == 0L) {
                // DB file missing or empty: return failure
                (context as? MainActivity)?.runOnUiThread {
                    if (!autoBackup)
                        context.showSnackbar(context.getString(R.string.birday_export_failure))
                    else
                        Toast.makeText(
                            context,
                            context.getString(R.string.birday_export_failure),
                            Toast.LENGTH_SHORT
                        ).show()
                }
                return ""
            }
            try {
                if (uri != null) {
                    // Write to the SAF Uri provided by the user
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        dbFile.inputStream().use { fis ->
                            fis.copyTo(os)
                            os.flush()
                        }
                    } ?: throw IOException("Cannot open output stream for uri: $uri")
                    // Optional: try taking persistable permission (may or may not be supported)
                    try {
                        val takeFlags =
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                    } catch (_: Exception) {
                        // not critical — ignore if provider doesn't allow persistable permission
                    }
                    // Notify user on UI thread
                    (context as? MainActivity)?.runOnUiThread {
                        if (autoBackup)
                            Toast.makeText(
                                context,
                                context.getString(R.string.birday_export_success),
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                    // Return the uri string so the caller can share it
                    return uri.toString()
                } else {
                    // Legacy behavior: write to app files dir
                    val appDirectory = File(context.getExternalFilesDir(null)!!.absolutePath)
                    val fileName =
                        if (autoBackup) "BirdayBackup_auto.db" else "BirdayBackup_${LocalDate.now()}.db"
                    val destFile = File(appDirectory, fileName)
                    dbFile.copyTo(destFile, overwrite = true)
                    (context as? MainActivity)?.runOnUiThread {
                        if (autoBackup)
                            Toast.makeText(
                                context,
                                context.getString(R.string.birday_export_success),
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                    return destFile.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (context as? MainActivity)?.runOnUiThread {
                    if (autoBackup)
                        Toast.makeText(
                            context,
                            context.getString(R.string.birday_export_failure),
                            Toast.LENGTH_SHORT
                        ).show()
                }
                return ""
            }
        }
    }
}