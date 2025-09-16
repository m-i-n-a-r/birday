package com.minar.birday.preferences.standard

import android.annotation.SuppressLint
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
import androidx.core.net.toUri


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
        binding.batteryOptimizationsDescription.text =
            context.getString(R.string.battery_optimization_description)
    }

    @SuppressLint("BatteryLife")
    override fun onClick(v: View) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val packageName = context.packageName

            // Already in allow list
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(context, context.getString(android.R.string.ok), Toast.LENGTH_LONG)
                    .show()
                return
            }

            // Launch dialog to ask to ignore optimizations
            val requestIntent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:$packageName".toUri()
            )

            // Fallback for unavailable intent
            if (requestIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(requestIntent)
            } else {
                val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(settingsIntent)
                Toast.makeText(
                    context,
                    context.getString(R.string.battery_optimization_tutorial),
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.wtf), Toast.LENGTH_LONG).show()
        }
    }

}
