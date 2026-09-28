package com.minar.birday.utils

import com.minar.birday.utilities.formatRepublicanDate
import com.minar.birday.utilities.getNextRepublicanBirthday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RepublicanCalendarUnitTest {
    @Test
    fun nextRepublicanBirthday_keepsRepublicanDateAndIsNotBeforeToday() {
        val birthday = LocalDate.of(1988, 4, 22)
        val today = LocalDate.of(2026, 1, 1)

        val nextBirthday = getNextRepublicanBirthday(birthday, today)

        assertFalse(nextBirthday.isBefore(today))
        assertEquals(
            formatRepublicanDate(birthday, null).substringBeforeLast(' '),
            formatRepublicanDate(nextBirthday, null).substringBeforeLast(' ')
        )
    }

    @Test
    fun nextRepublicanBirthday_includesTodayAndMovesToFollowingOccurrenceAfterward() {
        val birthday = LocalDate.of(1988, 4, 22)
        val firstOccurrence = getNextRepublicanBirthday(birthday, LocalDate.of(2026, 1, 1))

        assertEquals(firstOccurrence, getNextRepublicanBirthday(birthday, firstOccurrence))

        val followingOccurrence = getNextRepublicanBirthday(birthday, firstOccurrence.plusDays(1))
        assertTrue(followingOccurrence.isAfter(firstOccurrence))
        assertEquals(
            formatRepublicanDate(birthday, null).substringBeforeLast(' '),
            formatRepublicanDate(followingOccurrence, null).substringBeforeLast(' ')
        )
    }
}
