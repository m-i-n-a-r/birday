package com.minar.birday.utilities

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R

class ContactsImporter(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView

        /*val importer = v.findViewById<LinearLayout>(R.id.importContacts)
        importer.setOnClickListener {
            
        }*/
    }
}