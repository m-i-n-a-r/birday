package com.minar.birday.utils

import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.getNextUnbirthday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate


private fun event(
    date: LocalDate,
    yearMatter: Boolean = true,
    type: EventCode = EventCode.BIRTHDAY
) = EventResult(id = 0, type = type.name, name = "Test", yearMatter = yearMatter, originalDate = date)

// The slow and obvious answer, day by day, to check the month by month search against
private fun bruteForce(birth: LocalDate, from: LocalDate): LocalDate? =
    generateSequence(from) { it.plusDays(1) }
        .take(365 * 12)
        .firstOrNull { it.dayOfMonth == birth.dayOfMonth && it.dayOfWeek == birth.dayOfWeek }

class UnbirthdayUnitTest {

    @Test
    fun matches_the_day_by_day_search() {
        val births = listOf(
            LocalDate.of(1990, 7, 13),
            LocalDate.of(1985, 1, 31),
            LocalDate.of(2000, 2, 29),
            LocalDate.of(1972, 12, 30),
            LocalDate.of(2010, 6, 1),
        )
        var from = LocalDate.of(2026, 1, 1)
        repeat(60) {
            for (birth in births)
                assertEquals("$birth from $from", bruteForce(birth, from), getNextUnbirthday(event(birth), from))
            from = from.plusDays(23)
        }
    }

    @Test
    fun today_counts() {
        val birth = LocalDate.of(1990, 7, 13)
        val friday13 = bruteForce(birth, LocalDate.of(2026, 1, 1))!!
        assertEquals(friday13, getNextUnbirthday(event(birth), friday13))
    }

    @Test
    fun no_answer_without_a_full_birth_date() {
        val birth = LocalDate.of(1990, 7, 13)
        val from = LocalDate.of(2026, 1, 1)
        assertNull(getNextUnbirthday(event(birth, yearMatter = false), from))
        assertNull(getNextUnbirthday(event(birth, type = EventCode.ANNIVERSARY), from))
        assertNull(getNextUnbirthday(event(LocalDate.of(2030, 1, 1)), from))
    }
}
