package com.minar.birday.persistence

import androidx.room.TypeConverter
import java.sql.Date
import java.sql.Date.valueOf
import java.time.LocalDate

class LocalDateTypeConverter {
    @TypeConverter
    fun DateToLocalDate(dateSql: Date?): LocalDate? {
        return dateSql.toLocalDate()
    }

    @TypeConverter
    fun localDateToDate(date: LocalDate?): Date? {
        return valueOf(date)
    }
}