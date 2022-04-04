package com.minar.birday.utilities

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.databinding.DisableOptimizationsRowBinding


// A custom preference to open the battery optimization settings
@ExperimentalStdlibApi
class DisableOptimizationsPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
    View.OnClickListener {
    private lateinit var binding: DisableOptimizationsRowBinding

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = DisableOptimizationsRowBinding.bind(holder.itemView)
        binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    }

}
