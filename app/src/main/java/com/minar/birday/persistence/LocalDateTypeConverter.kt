package com.minar.birday.persistence

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.room.TypeConverter
import java.lang.Exception
import java.time.LocalDate

class LocalDateTypeConverter {
    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate {
        // Extra steps to avoid crashes for leap years
        return try {
            if (value == null) LocalDate.now()
            else LocalDate.parse(value)
        } catch (_: Exception) {
            if (value!!.substring(5) == "02-29") {
                if (useFebruary28)
                    LocalDate.parse("${value.substring(0, 5)}02-28")
                else
                    LocalDate.parse("${value.substring(0, 5)}03-01")
            } else LocalDate.now()
        }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }

    companion object {
        const val PREFERENCE_KEY = "leap_year_feb28"

        // When true, Feb 29 birthdays fall on Feb 28 in non-leap years; otherwise on Mar 01.
        // Room instantiates the converter itself, so the flag can't be injected: it is refreshed by
        // loadPreference() from EventDatabase.getBirdayDatabase(), and written directly by the
        // settings screen so the change is visible before the list reloads.
        @Volatile
        var useFebruary28: Boolean = false

        // Widgets and the notification worker can read the database without the app ever being
        // opened, so relying on MainActivity alone would make them disagree with the app
        fun loadPreference(context: Context) {
            useFebruary28 = PreferenceManager
                .getDefaultSharedPreferences(context.applicationContext)
                .getBoolean(PREFERENCE_KEY, false)
        }
    }
}