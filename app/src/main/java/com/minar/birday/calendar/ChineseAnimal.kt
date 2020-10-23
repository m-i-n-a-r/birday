package com.minar.birday.calendar

import android.icu.util.*
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Adapter to use either Android's provided ICU or the original one.
 *
 * This is required as:
 * - In runtime, ICU is already available on the Android devices under
 *   android.icu.* and doesn't require extra dependencies.
 * - In tests, all functions, including ICU, aren't available
 *   (https://g.co/androidstudio/not-mocked), so the actual ICU library
 *   is needed as a test dependency.
 */
interface CalendarAdapter {
    /**
     * Wrapper for Calendar.setTimeInMillis.
     */
    fun setTimeInMillis(value: Long)

    /**
     * Wrapper for Calendar.get.
     */
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

/**
 * Implementation of calendar adapter for Android-provided ICU.
 */
class AndroidCalendar(private val calendar: Calendar): CalendarAdapter {
    override fun setTimeInMillis(value: Long) {
        calendar.timeInMillis = value
    }

    override fun get(field: Int): Int = calendar.get(field)
}

/**
 * Finds the Chinese calendar year animal corresponding to the given date.
 * @returns Animal index, starting from 0 = rat
 */
fun chineseAnimal(date: LocalDate): Int = chineseAnimalGeneric(
    getChinese = { AndroidCalendar(ChineseCalendar()) },
    date
)
