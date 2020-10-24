package com.minar.birday.calendar

import android.icu.util.*
import java.time.LocalDate
import java.time.ZoneOffset

// Adapter to use either Android's provided ICU or the original one.
interface CalendarAdapter {

    // Wrapper for Calendar.setTimeInMillis
    fun setTimeInMillis(value: Long)

    // Wrapper for Calendar.get
    fun get(field: Int): Int
}

/**
 * Finds the Chinese calendar year animal corresponding to the given date, using the provided
 * ICU implementation.
 * @param getChinese Function to create a Chinese calendar
 * @returns Animal index, starting from 0 = rat
 */
fun<C: CalendarAdapter> chineseAnimalGeneric(getChinese: () -> C, date: LocalDate): Int {
    // Convert the given date to a UNIX epoch value, assuming Beijing time
    val millis = date.atTime(12, 0)
        .toInstant(ZoneOffset.ofHours(8))
        .toEpochMilli()
    // Create a Chinese calendar instance for the given epoch value
    val chinese = getChinese()
    chinese.setTimeInMillis(millis)
    // Extract the year, which corresponds to the animal cycle
    val year = chinese.get(Calendar.YEAR)
    return (year - 1) % 12
}

// Implementation of calendar adapter for Android-provided ICU.
class AndroidCalendar(private val calendar: Calendar): CalendarAdapter {
    override fun setTimeInMillis(value: Long) {
        calendar.timeInMillis = value
    }
    override fun get(field: Int): Int = calendar.get(field)
}

// return the Animal index (starting from 0 = rat) corresponding to the given date
fun chineseAnimal(date: LocalDate): Int = chineseAnimalGeneric(
    getChinese = { AndroidCalendar(ChineseCalendar()) },
    date
)
