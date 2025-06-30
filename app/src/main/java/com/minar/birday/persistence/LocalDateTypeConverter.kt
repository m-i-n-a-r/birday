package com.minar.birday.persistence

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
            if (value!!.substring(5) == "02-29")
                LocalDate.parse("${value.substring(0,5)}03-01")
            else LocalDate.now()
        }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }
}