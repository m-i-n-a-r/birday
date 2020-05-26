package com.minar.birday.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.MainActivity
import com.minar.birday.R
import java.io.File


class BirdayImporter(context: Context?, attrs: AttributeSet?) : Preference(context, attrs), View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    // Vibrate and import the backup if possible
    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()
        chooseFile(context)
    }

    // Import a backup overwriting any existing data and checking if the file is valid
    fun importBirthdays(context: Context, fileUri: Uri): Boolean {
        if (!isBackupValid(context, fileUri)) {
            Toast.makeText(context, context.getString(R.string.birday_import_invalid_file), Toast.LENGTH_SHORT).show()
            return false
        }
        val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
        try {
            File(fileUri.toString()).copyTo(dbFile, true)
            Toast.makeText(context, context.getString(R.string.birday_import_success), Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.birday_import_failure), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return true
    }

    // Start an intent to choose a file
    private fun chooseFile(context: Context) {
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
        fileIntent.type = "*/*"
        // Verify that the intent will resolve to an activity
        if (fileIntent.resolveActivity(context.packageManager) != null)
            context.startActivity(fileIntent)
    }

    // Check if a backup file is valid using various strategies. A wrong import would result in a crash
    private fun isBackupValid(context: Context, fileUri: Uri): Boolean {
        return fileUri.toString().contains("BirdayBackup_")
    }

}