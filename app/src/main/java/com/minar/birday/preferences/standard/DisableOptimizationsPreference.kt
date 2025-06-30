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
import com.minar.birday.databinding.DisableOptimizationsRowBinding


// A custom preference to open the battery optimization settings
class DisableOptimizationsPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
    View.OnClickListener {
    private lateinit var binding: DisableOptimizationsRowBinding

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = DisableOptimizationsRowBinding.bind(holder.itemView)
        binding.root.setOnClickListener(this)
        // Include the tutorial in the description itself, since the toast can be unreadable with big text sizes
        binding.batteryOptimizationsDescription.text = buildString {
            append(context.getString(R.string.battery_optimization_description))
            append("\n\n")
            append(context.getString(R.string.battery_optimization_tutorial))
        }
    }

    override fun onClick(v: View) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
            // Double tasty toast for a longer reading time
            Toast.makeText(
                context,
                context.getString(R.string.battery_optimization_tutorial),
                Toast.LENGTH_LONG
            ).show()
            Toast.makeText(
                context,
                context.getString(R.string.battery_optimization_tutorial),
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            Toast.makeText(
                context, context.getString(R.string.wtf), Toast.LENGTH_LONG
            ).show()
        }
    }

}
