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
import com.minar.birday.persistence.EventDatabase
import java.io.*
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
        val exported = exportBirthdays(context)
        shareBackup(exported)
    }

    // Export the room database to a file in Android/data/com.minar.birday/files
    private fun exportBirthdays(context: Context): String {
        val birdayDB: EventDatabase? = EventDatabase.getBirdayDataBase(context)
        birdayDB!!.close()
        val dbFile: File = context.getDatabasePath("BirdayDB")
        val directory = File(context.getExternalFilesDir(null)!!.absolutePath)
        val fileName: String = "BirdayBackup_" + LocalDate.now()
        val fileFullPath: String = directory.path + File.separator.toString() + fileName
        if (!directory.exists()) directory.mkdirs()
        val saveFile = File(fileFullPath)
        if (saveFile.exists()) saveFile.delete() // Overwrite if existing
        try {
            if (saveFile.createNewFile()) {
                val bufferSize = 8 * 1024
                var bytesRead: Int
                val buffer = ByteArray(bufferSize)
                val saveDb: OutputStream = FileOutputStream(fileFullPath)
                val inDb: InputStream = FileInputStream(dbFile)
                while (inDb.read(buffer, 0, bufferSize).also { bytesRead = it } > 0) saveDb.write(buffer, 0, bytesRead)
                saveDb.flush()
                inDb.close()
                saveDb.close()
                Toast.makeText(context, context.getString(R.string.birday_export_success), Toast.LENGTH_SHORT).show()
            }
        }
        catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.birday_export_failure), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return fileFullPath
    }

    // Share the backup to a supported app
    private fun shareBackup(fileUri: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse(fileUri))
            type = "*/*"
        }

        // Verify that the intent will resolve to an activity
        if (shareIntent.resolveActivity(context.packageManager) != null)
            context.startActivity(shareIntent)
    }
}