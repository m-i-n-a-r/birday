package com.minar.birday.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider.getUriForFile
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.sqlite.db.SimpleSQLiteQuery
import com.minar.birday.activities.MainActivity
import com.minar.birday.R
import com.minar.birday.persistence.EventDatabase
import java.io.File
import java.time.LocalDate


class BirdayExporter(context: Context?, attrs: AttributeSet?) : Preference(context, attrs), View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    // Vibrate, export a backup and immediately share it if possible
    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()
        val thread = Thread {
            val exported = exportBirthdays(context)
            if (exported.isNotBlank()) shareBackup(exported)
        }
        thread.start()
    }

    // Export the room database to a file in Android/data/com.minar.birday/files
    private fun exportBirthdays(context: Context): String {
        // Perform a checkpoint to empty the write ahead logging temporary files and avoid closing the entire db
        val eventDao = EventDatabase.getBirdayDataBase(context)!!.eventDao()
        eventDao.checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))

        val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
        val appDirectory = File(context.getExternalFilesDir(null)!!.absolutePath)
        val fileName: String = "BirdayBackup_" + LocalDate.now()
        val fileFullPath: String = appDirectory.path + File.separator.toString() + fileName
        // Toasts need the ui thread to work, so they must be forced on that thread
        try {
            dbFile.copyTo(File(fileFullPath), true)
            (context as MainActivity).runOnUiThread { Toast.makeText(context, context.getString(R.string.birday_export_success), Toast.LENGTH_SHORT).show() }
        }
        catch (e: Exception) {
            (context as MainActivity).runOnUiThread { Toast.makeText(context, context.getString(R.string.birday_export_failure), Toast.LENGTH_SHORT).show() }
            e.printStackTrace()
            return ""
        }
        return fileFullPath
    }

    // Share the backup to a supported app
    private fun shareBackup(fileUri: String) {
        val file = File(fileUri)
        val contentUri: Uri = getUriForFile(context, "com.minar.birday.fileprovider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "*/*"
        }
        // Verify that the intent will resolve to an activity
        if (shareIntent.resolveActivity(context.packageManager) != null)
            context.startActivity(shareIntent)
    }
}