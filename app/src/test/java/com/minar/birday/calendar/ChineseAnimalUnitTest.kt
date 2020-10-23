package com.minar.birday.calendar

import com.ibm.icu.util.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Implementation of calendar adapter for original ICU.
 */
class ICUCalendar(private val calendar: Calendar): CalendarAdapter {
    override fun setTimeInMillis(value: Long) {
        calendar.timeInMillis = value
    }

    override fun get(field: Int): Int = calendar.get(field)
}

fun chineseAnimalTest(date: LocalDate): Int = chineseAnimalGeneric(
    getChinese = { ICUCalendar(ChineseCalendar()) },
    date
)

class ChineseAnimalUnitTest {
    // 2020 is the year of the Rat (0)
    // However, it started on January 25th, so January 10 is still in the year of the Pig (11)

    @Test
    fun chineseAnimal_is_pig_2020_early_January() {
        assertEquals(11, chineseAnimalTest(LocalDate.of(2020, 1, 20)))
    }

    @Test
    fun chineseAnimal_is_rat_2020_late_January() {
        assertEquals(0, chineseAnimalTest(LocalDate.of(2020, 1, 28)))
    }

    @Test
    fun chineseAnimal_is_rat_2020_June() {
        assertEquals(0, chineseAnimalTest(LocalDate.of(2020, 6, 1)))
    }
}
