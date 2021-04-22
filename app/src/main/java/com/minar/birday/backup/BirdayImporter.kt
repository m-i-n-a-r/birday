package com.minar.birday.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.EventDatabase
import java.io.FileOutputStream
import java.io.InputStreamReader


class BirdayImporter(context: Context?, attrs: AttributeSet?) : Preference(context, attrs),
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
        act.selectBackup.launch("*/*")
    }

    // Import a backup overwriting any existing data and checking if the file is valid
    fun importBirthdays(context: Context, fileUri: Uri): Boolean {
        if (!isBackupValid(fileUri)) {
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_invalid_file))
            return false
        }
        EventDatabase.destroyInstance()
        val fileStream = context.contentResolver.openInputStream(fileUri)!!
        val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
        try {
            fileStream.copyTo(FileOutputStream(dbFile))
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_success))
        } catch (e: Exception) {
            (context as MainActivity).showSnackbar(context.getString(R.string.birday_import_failure))
            e.printStackTrace()
            return false
        }
        // Completely restart the application with a slight delay to be extra-safe
        val intent: Intent =
            act.baseContext.packageManager.getLaunchIntentForPackage(act.baseContext.packageName)!!
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        Handler(Looper.getMainLooper()).postDelayed({ act.startActivity(intent) }, 400)
        return true
    }

    // Check if a backup file is valid. A wrong import would result in a crash or empty db
    private fun isBackupValid(fileUri: Uri): Boolean {
        val uri = fileUri.path ?: ""

        // An initial, naive validation
        if (!(uri.contains("birdaybackup", true) ||
                    uri.contains("document", true))
        )
            return false

        // Read the first bytes of the file: every SQLite DB starts with the same string
        val fileStream = context.contentResolver.openInputStream(fileUri)!!
        try {
            val fr = InputStreamReader(fileStream)
            val buffer = CharArray(16)
            fr.read(buffer, 0, 16)
            val str = String(buffer)
            fr.close()
            if (str == "SQLite format 3\u0000") return true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return false
        }
        return false
    }

}