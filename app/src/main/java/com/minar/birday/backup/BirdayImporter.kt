package com.minar.birday.backup

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.MainActivity
import com.minar.birday.R
import java.io.FileOutputStream


class BirdayImporter(context: Context?, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    // Vibrate and import the backup if possible
    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()
        act.selectBackup.launch("*/*")
    }

    // Import a backup overwriting any existing data and checking if the file is valid
    fun importBirthdays(context: Context, fileUri: Uri): Boolean {
        if (!isBackupValid(fileUri)) {
            Toast.makeText(context, context.getString(R.string.birday_import_invalid_file), Toast.LENGTH_SHORT).show()
            return false
        }
        val fileStream = context.contentResolver.openInputStream(fileUri)!!
        val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
        try {
            fileStream.copyTo(FileOutputStream(dbFile))
            Toast.makeText(context, context.getString(R.string.birday_import_success), Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.birday_import_failure), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return false
        }
        (context as MainActivity).recreate()
        return true
    }

        // Check if a backup file is valid using various strategies. A wrong import would result in a crash
        private fun isBackupValid(fileUri: Uri): Boolean {
            return fileUri.toString().contains("BirdayBackup_")
        }

    }