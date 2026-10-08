package com.minar.birday.preferences.standard

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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.core.content.edit

// A custom preference to show a time picker. Without a key it's the time of the notifications,
// with one it's another time, kept in "<key>_hour" and "<key>_minute" and starting from that one
class TimePickerPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var currentHour: String
    private lateinit var currentMinute: String
    private lateinit var binding: TimePickerRowBinding
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val main get() = key == null
    private val hourKey get() = if (main) "notification_hour" else "${key}_hour"
    private val minuteKey get() = if (main) "notification_minute" else "${key}_minute"

    private fun readTime() {
        val mainHour = sharedPrefs.getString("notification_hour", "8").toString()
        val mainMinute = sharedPrefs.getString("notification_minute", "0").toString()
        currentHour = sharedPrefs.getString(hourKey, mainHour).toString()
        currentMinute = sharedPrefs.getString(minuteKey, mainMinute).toString()
    }

    // The main time carries the warning about the systems that kill the app, the others only the time
    private fun describe(time: LocalTime): String {
        val formatted = "~${formatter.format(time)}"
        return if (main) String.format(context.getString(R.string.notification_hour_description), formatted)
        else formatted
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        readTime()
        super.onBindViewHolder(holder)
        binding = TimePickerRowBinding.bind(holder.itemView)
        if (!main) binding.timePickerTitle.text = title

        binding.timePickerDescription.text =
            describe(LocalTime.of(currentHour.toInt(), currentMinute.toInt()))

        binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        readTime()

        // Show the time picker
        val isSystem24Hour = is24HourFormat(context)
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(currentHour.toInt())
                .setMinute(currentMinute.toInt())
                .setTitleText(if (main) context.getString(R.string.notification_hour_name) else title)
                .build()

        picker.addOnPositiveButtonClickListener {
            sharedPrefs.edit {
                putString(hourKey, "${picker.hour}")
                putString(minuteKey, "${picker.minute}")
            }

            // Format the selected hour and update the text
            binding.timePickerDescription.text = describe(LocalTime.of(picker.hour, picker.minute))
        }

        picker.show(act.supportFragmentManager, "timepicker")
    }

}
