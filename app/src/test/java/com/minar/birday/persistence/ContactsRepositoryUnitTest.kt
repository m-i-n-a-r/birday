package com.minar.birday.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeParseException


class ContactsRepositoryUnitTest {

    @Test
    fun compact_and_extended_dates_import_the_same_birthday() {
        val expected = LocalDate.of(1990, 5, 4)
        assertEquals(expected, parseContactDate("19900504"))
        assertEquals(expected, parseContactDate("1990-05-04"))
    }

    @Test
    fun leap_days_are_preserved() {
        val expected = LocalDate.of(2000, 2, 29)
        assertEquals(expected, parseContactDate("20000229"))
        assertEquals(expected, parseContactDate("2000-02-29"))
    }

    @Test
    fun normalized_yearless_dates_still_parse() {
        assertEquals(LocalDate.of(1970, 5, 4), parseContactDate("1970-05-04"))
    }

    @Test
    fun invalid_dates_are_rejected_instead_of_adjusted() {
        listOf("19900229", "1990-02-29", "19900431", "1990-04-31",
            "19901301", "1990-13-01", "1990054", "", "not-a-date").forEach { value ->
            assertThrows(DateTimeParseException::class.java) { parseContactDate(value) }
        }
    }
}
