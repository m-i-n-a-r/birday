package com.minar.birday.preferences.standard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.databinding.NotificationSoundRowBinding


// A custom preference to open the notification sound settings for Birday
@ExperimentalStdlibApi
class NotificationSoundPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
    View.OnClickListener {
    private lateinit var binding: NotificationSoundRowBinding

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = NotificationSoundRowBinding.bind(holder.itemView)
        binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        try {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, "events_channel")
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context, context.getString(R.string.wtf), Toast.LENGTH_LONG
            ).show()
        }
    }

}
