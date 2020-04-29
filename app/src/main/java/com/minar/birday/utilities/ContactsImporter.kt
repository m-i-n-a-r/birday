package com.minar.birday.utilities

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.MainActivity
import com.minar.birday.R

class ContactsImporter(context: Context?, attrs: AttributeSet?) : Preference(context, attrs), View.OnClickListener {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()
        if(act.importContacts()) Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, context.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()

    }
}