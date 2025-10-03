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
            // TODO At the moment, the autobackup has the same name of the last saved manual backup
            val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            // Checkpoint WAL to flush DB to disk
            eventDao.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
            val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
            // Quick sanity checks
            if (!dbFile.exists() || dbFile.length() == 0L) {
                // DB file missing or empty: return failure
                handleResult(autoBackup, false, context, false)
                return ""
            }
            try {
                if (uri != null  && uri.toString().isNotBlank()) {
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
                        // Not critical — ignore if provider doesn't allow persistable permission
                    }
                    // Notify user on UI thread
                    handleResult(autoBackup, true, context, true)
                    // Return the uri string so the caller can share it
                    return uri.toString()
                } else {
                    // Legacy behavior: write to app files dir
                    val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
                    val appDirectory = File(context.getExternalFilesDir(null)!!.absolutePath)
                    val fileName =
                        if (autoBackup) "BirdayBackup_auto" else "BirdayBackup_${LocalDate.now()}"
                    val fileFullPath: String = appDirectory.path + File.separator + fileName
                    // Snackbar need the UI thread to work, so they must be forced on that thread
                    try {
                        dbFile.copyTo(File(fileFullPath), true)
                        handleResult(autoBackup, true, context, false)
                    } catch (e: Exception) {
                        handleResult(autoBackup, false, context, false)
                        e.printStackTrace()
                        return ""
                    }
                    return fileFullPath
                }
            } catch (e: Exception) {
                e.printStackTrace()
                handleResult(autoBackup, false, context, true)
                return ""
            }
        }
        // Show a toast or a snackbar depending on the result
        private fun handleResult(
            autoBackup: Boolean = false,
            success: Boolean = true,
            context: Context,
            onlyToast: Boolean = false
        ) {
            val resultString =
                if (success) context.getString(R.string.birday_export_success)
                else context.getString(R.string.birday_export_failure)
            (context as? MainActivity)?.runOnUiThread {
                if (!autoBackup && !onlyToast)
                    context.showSnackbar(resultString)
                else {
                    Toast.makeText(
                        context,
                        resultString,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}