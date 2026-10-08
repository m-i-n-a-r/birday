package com.minar.birday.utils

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import com.minar.birday.utilities.EventCalendar
import com.minar.birday.utilities.nextOccurrenceGeneric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset


// The original ICU in place of Android's, same algorithm
private fun icu(type: String) =
    ICUCalendar(Calendar.getInstance(TimeZone.GMT_ZONE, ULocale("@calendar=$type")))

private fun next(date: LocalDate, calendar: EventCalendar, from: LocalDate) =
    nextOccurrenceGeneric(::icu, date, calendar, from)

// Month, leap flag and day of a Gregorian date in the given calendar
private fun fields(date: LocalDate, calendar: EventCalendar): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(TimeZone.GMT_ZONE, ULocale("@calendar=${calendar.key}"))
    cal.timeInMillis = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    return Triple(cal.get(Calendar.MONTH), cal.get(Calendar.IS_LEAP_MONTH), cal.get(Calendar.DAY_OF_MONTH))
}

class CalendarSystemsUnitTest {

    @Test
    fun chinese_new_year_comes_back_on_the_next_new_year() {
        // Lunar new year 2023 was January 22nd, the 2026 one fell on February 17th. Not 2027:
        // its new moon is minutes from midnight in Beijing, and ICU's approximation puts the new
        // year on the 7th where the official calendar says the 6th
        val born = LocalDate.of(2023, 1, 22)
        assertEquals(LocalDate.of(2026, 2, 17), next(born, EventCalendar.CHINESE, LocalDate.of(2025, 9, 28)))
        assertEquals(LocalDate.of(2026, 2, 17), next(born, EventCalendar.CHINESE, LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun the_day_itself_counts() {
        val born = LocalDate.of(2023, 1, 22)
        val newYear = LocalDate.of(2026, 2, 17)
        assertEquals(newYear, next(born, EventCalendar.CHINESE, newYear))
    }

    @Test
    fun matches_the_day_by_day_search_in_every_calendar() {
        // Days up to the 28th exist in every month of every calendar here, so a plain search
        // for the same month and day is the right answer
        val births = listOf(
            LocalDate.of(1950, 3, 14), LocalDate.of(1972, 11, 2), LocalDate.of(1990, 7, 13),
            LocalDate.of(2001, 1, 5), LocalDate.of(2015, 9, 30),
        )
        for (calendar in EventCalendar.entries) for (birth in births) {
            val (month, leap, day) = fields(birth, calendar)
            if (day > 28 || leap == 1) continue
            if (calendar == EventCalendar.HEBREW && month == 5) continue
            var from = LocalDate.of(2026, 1, 1)
            repeat(12) {
                val expected = generateSequence(from) { it.plusDays(1) }.take(800)
                    .first { fields(it, calendar) == Triple(month, 0, day) }
                assertEquals("$calendar $birth from $from", expected, next(birth, calendar, from))
                from = from.plusDays(61)
            }
        }
    }

    @Test
    fun missing_chinese_leap_month_becomes_the_regular_one() {
        // 2023 had a leap second month: March 25th is its fourth day. 2027 has none
        val born = LocalDate.of(2023, 3, 25)
        assertEquals(Triple(1, 1, 4), fields(born, EventCalendar.CHINESE))
        val result = next(born, EventCalendar.CHINESE, LocalDate.of(2026, 9, 28))
        assertEquals(Triple(1, 0, 4), fields(result, EventCalendar.CHINESE))
        assertTrue(result.isBefore(LocalDate.of(2027, 6, 1)))
    }

    @Test
    fun hebrew_adar_i_falls_on_adar_in_a_common_year() {
        // February 20th 2024 is in Adar I of 5784, a leap year. 5786 is a common one
        val born = LocalDate.of(2024, 2, 20)
        val (month, _, day) = fields(born, EventCalendar.HEBREW)
        assertEquals(5, month)
        val result = next(born, EventCalendar.HEBREW, LocalDate.of(2025, 10, 1))
        assertEquals(Triple(6, 0, day), fields(result, EventCalendar.HEBREW))
        // 5787 is a leap year again: Adar I is back
        val leap = next(born, EventCalendar.HEBREW, LocalDate.of(2026, 9, 28))
        assertEquals(Triple(5, 0, day), fields(leap, EventCalendar.HEBREW))
    }

    @Test
    fun a_thirtieth_becomes_the_last_day_of_a_shorter_month() {
        val calendar = EventCalendar.ISLAMIC_CIVIL
        // Find a birth on the 30th of a month
        val born = generateSequence(LocalDate.of(1990, 1, 1)) { it.plusDays(1) }
            .first { fields(it, calendar).third == 30 }
        val (month, _, _) = fields(born, calendar)
        var from = LocalDate.of(2026, 1, 1)
        repeat(6) {
            val result = next(born, calendar, from)
            val (resultMonth, _, resultDay) = fields(result, calendar)
            assertEquals(month, resultMonth)
            // Either the 30th itself, or the last day of a month that stops at 29
            assertTrue(resultDay == 30 || fields(result.plusDays(1), calendar).first != resultMonth)
            assertFalse(result.isBefore(from))
            from = result.plusDays(1)
        }
    }
}
