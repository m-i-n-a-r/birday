package com.minar.birday.utils

import com.minar.birday.utilities.weekendReminderDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate


class WeekendReminderUnitTest {
    private val saturdaySunday = { date: LocalDate ->
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
    private val fridaySaturday = { date: LocalDate ->
        date.dayOfWeek == DayOfWeek.FRIDAY || date.dayOfWeek == DayOfWeek.SATURDAY
    }

    @Test
    fun saturday_and_sunday_are_reminded_on_friday() {
        val friday = LocalDate.of(2026, 10, 2)
        assertEquals(friday, weekendReminderDay(LocalDate.of(2026, 10, 3), saturdaySunday))
        assertEquals(friday, weekendReminderDay(LocalDate.of(2026, 10, 4), saturdaySunday))
    }

    @Test
    fun a_working_day_has_no_reminder() {
        assertNull(weekendReminderDay(LocalDate.of(2026, 10, 5), saturdaySunday))
    }

    @Test
    fun the_weekend_follows_the_region() {
        // Where the weekend is Friday and Saturday, the last working day is Thursday
        val thursday = LocalDate.of(2026, 10, 1)
        assertEquals(thursday, weekendReminderDay(LocalDate.of(2026, 10, 2), fridaySaturday))
        assertEquals(thursday, weekendReminderDay(LocalDate.of(2026, 10, 3), fridaySaturday))
    }

    @Test
    fun a_weekend_without_end_never_hangs() {
        assertNull(weekendReminderDay(LocalDate.of(2026, 10, 3)) { true })
    }
}
