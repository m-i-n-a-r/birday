package com.minar.birday.utilities

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat.is24HourFormat
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.TimePickerRowBinding

// A custom preference to show a time picker
class TimePickerPreference(context: Context?, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var currentTime: String
    private lateinit var binding: TimePickerRowBinding

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        currentTime = sharedPrefs.getString("notification_hour", "8").toString()
        super.onBindViewHolder(holder)
        binding = TimePickerRowBinding.bind(holder.itemView)

        binding.timePickerDescription.text =
            String.format(context.getString(R.string.notification_hour_description), currentTime)
        binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        currentTime = sharedPrefs.getString("notification_hour", "8").toString()
        val isSystem24Hour = is24HourFormat(context)
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(currentTime.toInt())
                .setMinute(0)
                .setTitleText(context.getString(R.string.notification_hour_name))
                .build()

        picker.addOnPositiveButtonClickListener {
            sharedPrefs.edit().putString("notification_hour", picker.hour.toString()).apply()
            binding.timePickerDescription.text =
                String.format(
                    context.getString(R.string.notification_hour_description),
                    picker.hour.toString()
                )
        }

        picker.show(act.supportFragmentManager, "timepicker")
    }

}
