package com.minar.birday.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.FragmentSettingsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomAuthorPreference(context: Context?, attrs: AttributeSet?) :
    Preference(context, attrs), View.OnClickListener {
    private val activityScope = CoroutineScope(Dispatchers.Main)

    // Easter egg stuff, why not
    private var easterEgg = 0

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = FragmentSettingsBinding.bind(holder.itemView)

        // Manage the shimmer
        val shimmer = binding.settingsShimmer
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        if (shimmerEnabled) shimmer.startShimmer()

        // Make the icons clickable
        val logo = binding.imageMinar
        val l1 = binding.minarig
        val l2 = binding.minartt
        val l3 = binding.minarps
        val l4 = binding.minargit
        val l5 = binding.minarsite

        // Spawn the logo with a little delay
        activityScope.launch {
            delay(300)
            (logo.drawable as AnimatedVectorDrawable).start()
        }
        logo.setOnClickListener(this)
        l1.setOnClickListener(this)
        l2.setOnClickListener(this)
        l3.setOnClickListener(this)
        l4.setOnClickListener(this)
        l5.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        // Vibrate using the common method in MainActivity
        val act = context as Activity
        val uri: Uri
        when (v.id) {
            R.id.imageMinar -> if (easterEgg == 5) {
                Toast.makeText(context, context.getString(R.string.easter_egg), Toast.LENGTH_SHORT)
                    .show()
                easterEgg = 0
            } else easterEgg++
            R.id.minarig -> {
                if (act is MainActivity) act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_instagram))
                val intent1 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent1)
            }
            R.id.minartt -> {
                if (act is MainActivity) act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_twitter))
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