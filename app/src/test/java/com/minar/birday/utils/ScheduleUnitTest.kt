package com.minar.birday.utils

import com.minar.birday.utilities.delayToNextCheck
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime


private fun at(zone: String, dateTime: String): ZonedDateTime =
    LocalDateTime.parse(dateTime).atZone(ZoneId.of(zone))

class ScheduleUnitTest {

    @Test
    fun a_normal_day_is_24_hours() {
        val now = at("Europe/Rome", "2026-05-10T08:00:15")
        assertEquals(24 * 60 * 60L, delayToNextCheck(8, 0, now).seconds)
    }

    @Test
    fun later_today_if_not_yet_passed() {
        val now = at("Europe/Rome", "2026-05-10T07:00:00")
        assertEquals(60 * 60L + 15, delayToNextCheck(8, 0, now).seconds)
    }

    @Test
    fun midnight_check_skips_the_repeated_23_o_clock() {
        // Brazil, February 17 2018: at midnight the clocks went back to 23, the case of #365. The
        // next check has to be the next midnight, 25 hours later, not the second 23 o'clock
        val now = at("America/Sao_Paulo", "2018-02-17T00:00:15")
        val next = now.plus(delayToNextCheck(0, 0, now))
        assertEquals(LocalDateTime.parse("2018-02-18T00:00:15"), next.toLocalDateTime())
        assertEquals(25 * 60 * 60L, delayToNextCheck(0, 0, now).seconds)
    }

    @Test
    fun the_day_the_clocks_go_forward_is_23_hours() {
        val now = at("Europe/Rome", "2026-03-28T08:00:15")
        val next = now.plus(delayToNextCheck(8, 0, now))
        assertEquals(LocalDateTime.parse("2026-03-29T08:00:15"), next.toLocalDateTime())
        assertEquals(23 * 60 * 60L, delayToNextCheck(8, 0, now).seconds)
    }
}
