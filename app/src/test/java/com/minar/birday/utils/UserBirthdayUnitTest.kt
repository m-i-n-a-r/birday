package com.minar.birday.utils

import com.minar.birday.persistence.LocalDateTypeConverter
import com.minar.birday.utilities.isUserBirthday
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate


class UserBirthdayUnitTest {

    @After
    fun resetLeapRule() {
        LocalDateTypeConverter.useFebruary28 = false
    }

    @Test
    fun same_month_and_day_any_year() {
        val birthday = LocalDate.of(1990, 7, 13)
        assertTrue(isUserBirthday(birthday, LocalDate.of(2026, 7, 13)))
        assertFalse(isUserBirthday(birthday, LocalDate.of(2026, 7, 14)))
    }

    @Test
    fun february_29_follows_the_leap_rule() {
        val birthday = LocalDate.of(2000, 2, 29)
        assertTrue(isUserBirthday(birthday, LocalDate.of(2028, 2, 29)))

        LocalDateTypeConverter.useFebruary28 = false
        assertTrue(isUserBirthday(birthday, LocalDate.of(2027, 3, 1)))
        assertFalse(isUserBirthday(birthday, LocalDate.of(2027, 2, 28)))

        LocalDateTypeConverter.useFebruary28 = true
        assertTrue(isUserBirthday(birthday, LocalDate.of(2027, 2, 28)))
        assertFalse(isUserBirthday(birthday, LocalDate.of(2027, 3, 1)))
        // In a leap year the substitute day is just a normal day
        assertFalse(isUserBirthday(birthday, LocalDate.of(2028, 2, 28)))
    }
}
