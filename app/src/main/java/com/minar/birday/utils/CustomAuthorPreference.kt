package com.minar.birday.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.MainActivity
import com.minar.birday.R

class CustomAuthorPreference(
    context: Context?,
    attrs: AttributeSet?
) :
    Preference(context, attrs), View.OnClickListener {
    // Easter egg stuff, why not
    private var easterEgg = 0
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView

        // Make the icons clickable
        val logo = v.findViewById<ImageView>(R.id.imageMinar)
        val l1 = v.findViewById<ImageView>(R.id.minarig)
        val l2 = v.findViewById<ImageView>(R.id.minarpp)
        val l3 = v.findViewById<ImageView>(R.id.minarps)
        val l4 = v.findViewById<ImageView>(R.id.minargit)
        val l5 = v.findViewById<ImageView>(R.id.minarsite)
        logo.setOnClickListener(this)
        l1.setOnClickListener(this)
        l2.setOnClickListener(this)
        l3.setOnClickListener(this)
        l4.setOnClickListener(this)
        l5.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        // Vibrate and play sound using the common method in MainActivity
        val act = context as Activity
        val uri: Uri
        when (v.id) {
            R.id.imageMinar -> if (easterEgg == 5) {
                Toast.makeText(context, context.getString(R.string.easter_egg), Toast.LENGTH_SHORT).show()
                easterEgg = 0
            } else easterEgg++
            R.id.minarig -> {
                if (act is MainActivity) act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_instagram))
                val intent1 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent1)
            }
            R.id.minarpp -> {
                if (act is MainActivity) act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_paypal))
                val intent2 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent2)
            }
            R.id.minarps -> {
                if (act is MainActivity) act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_other_apps))
                val intent3 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent3)
            }
            R.id.minargit -> {
                if (act is MainActivity) act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_github))
                val intent4 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent4)
            }
            R.id.minarsite -> {
                if (act is MainActivity) act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_personal_site))
                val intent5 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent5)
            }
        }
    }
}