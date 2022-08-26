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


@ExperimentalStdlibApi
class CsvImporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
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
        act.selectBackup.launch("text/comma-separated-values")
    }

    // Import a backup overwriting any existing data and checking if the file is valid
    fun importEventsCsv(context: Context, fileUri: Uri): Boolean {
        EventDatabase.destroyInstance()
        val fileStream = context.contentResolver.openInputStream(fileUri)!!
        //val dbFile = context.getDatabasePath("BirdayDB").absoluteFile
        try {
            // Read the file as text
            // Find the date (or the oldest date, if more than one). If there's no date, skip the line
            // Check if the column names are in the file, in the smartest way possible
            // Create an entity for each valid line, with the columns in the correct position
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

}