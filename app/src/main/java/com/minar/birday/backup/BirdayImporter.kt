package com.minar.birday.backup

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.MainActivity


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
        importBirthdays(context)
    }

    // Import a backup selecting it manually and checking if the file is valid
    private fun importBirthdays(context: Context): Boolean {
        chooseFile(context)
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

}