package com.minar.birday.utils

import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.getDaysLived
import com.minar.birday.utilities.isDaysMilestone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate


private fun birthday(
    date: LocalDate,
    yearMatter: Boolean = true,
    type: EventCode = EventCode.BIRTHDAY
) = EventResult(
    id = 0,
    type = type.name,
    name = "Test",
    yearMatter = yearMatter,
    originalDate = date,
)

class DaysMilestoneUnitTest {
    private val born = LocalDate.of(2000, 1, 1)

    @Test
    fun daysLived_counts_from_the_birth() {
        assertEquals(0L, getDaysLived(birthday(born), born))
        assertEquals(1L, getDaysLived(birthday(born), born.plusDays(1)))
        // 2000 is a leap year: 366 days to the first birthday
        assertEquals(366L, getDaysLived(birthday(born), LocalDate.of(2001, 1, 1)))
    }

    @Test
    fun daysLived_is_null_without_a_meaningful_answer() {
        assertNull(getDaysLived(birthday(born, yearMatter = false), born.plusDays(10)))
        assertNull(getDaysLived(birthday(born, type = EventCode.ANNIVERSARY), born.plusDays(10)))
        assertNull(getDaysLived(birthday(born, type = EventCode.DEATH), born.plusDays(10)))
        // Born in the future
        assertNull(getDaysLived(birthday(born), born.minusDays(1)))
    }

    @Test
    fun milestone_every_thousand_days_but_not_the_birth() {
        assertFalse(isDaysMilestone(birthday(born), born))
        assertTrue(isDaysMilestone(birthday(born), born.plusDays(1000)))
        assertTrue(isDaysMilestone(birthday(born), born.plusDays(10_000)))
        assertFalse(isDaysMilestone(birthday(born), born.plusDays(999)))
        assertFalse(isDaysMilestone(birthday(born), born.plusDays(1001)))
    }

    @Test
    fun milestone_for_a_february_29_birth() {
        val leap = LocalDate.of(2004, 2, 29)
        assertTrue(isDaysMilestone(birthday(leap), leap.plusDays(3000)))
        assertEquals(LocalDate.of(2012, 5, 17), leap.plusDays(3000))
    }
}
