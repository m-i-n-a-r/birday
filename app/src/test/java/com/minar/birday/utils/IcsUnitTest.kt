package com.minar.birday.utils

import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.utilities.eventsToIcs
import com.minar.birday.utilities.icsToEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate


private val now = Instant.parse("2026-09-28T10:00:00Z")

private fun ics(vararg events: Event, lunar: (Event) -> List<LocalDate>? = { null }, february28: Boolean = false) =
    eventsToIcs(events.toList(), summary = { "${it.name} (${it.type})" }, lunarDates = lunar,
        february28 = february28, now = now)

class IcsUnitTest {

    @Test
    fun birday_events_come_back_as_they_were() {
        val events = listOf(
            Event(id = 0, type = EventCode.BIRTHDAY.name, name = "Mario", surname = "Rossi",
                originalDate = LocalDate.of(1990, 5, 4), notes = "Likes cake, and; commas\nand lines"),
            Event(id = 0, type = EventCode.NAME_DAY.name, name = "Anna", surname = "",
                originalDate = LocalDate.of(2026, 7, 26), yearMatter = false),
            Event(id = 0, type = EventCode.BIRTHDAY.name, name = "李", surname = "小龙",
                originalDate = LocalDate.of(1940, 11, 27), calendar = "chinese"),
        )
        val parsed = icsToEvents(ics(*events.toTypedArray()))
        assertEquals(events.size, parsed.size)
        events.zip(parsed).forEach { (original, back) ->
            assertEquals(original.name, back.name)
            assertEquals(original.surname, back.surname)
            assertEquals(original.type, back.type)
            assertEquals(original.originalDate, back.originalDate)
            assertEquals(original.yearMatter, back.yearMatter)
            assertEquals(original.notes, back.notes)
            assertEquals(original.calendar, back.calendar)
        }
    }

    @Test
    fun the_file_follows_the_standard() {
        val event = Event(id = 0, name = "A very long name that goes on", surname = "And a surname as long",
            originalDate = LocalDate.of(1990, 5, 4), notes = "x".repeat(200))
        val text = ics(event)
        val lines = text.split("\r\n")
        assertTrue(text.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(text.endsWith("END:VCALENDAR\r\n"))
        assertTrue(lines.all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertTrue("DTSTART;VALUE=DATE:19900504" in lines)
        assertTrue("RRULE:FREQ=YEARLY" in lines)
        // The same event gives the same identity, so a calendar updates it instead of doubling it
        assertEquals(lines.first { it.startsWith("UID:") }, ics(event).split("\r\n").first { it.startsWith("UID:") })
    }

    @Test
    fun the_picture_travels_only_when_asked() {
        val picture = ByteArray(3000) { (it * 7).toByte() }.also { it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte() }
        val event = Event(id = 0, name = "Pic", originalDate = LocalDate.of(1990, 5, 4), image = picture)
        val shared = eventsToIcs(listOf(event), summary = { it.name }, includeImages = true, now = now)
        assertTrue(shared.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertTrue(icsToEvents(shared).single().image!!.contentEquals(picture))
        val exported = eventsToIcs(listOf(event), summary = { it.name }, now = now)
        assertEquals(null, icsToEvents(exported).single().image)
    }

    @Test
    fun february_29_follows_the_setting() {
        val leap = Event(id = 0, name = "Leap", originalDate = LocalDate.of(1992, 2, 29))
        assertTrue("RRULE:FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=-1" in ics(leap, february28 = true).split("\r\n"))
        assertTrue("RRULE:FREQ=YEARLY;BYYEARDAY=60" in ics(leap, february28 = false).split("\r\n"))
    }

    @Test
    fun lunar_events_carry_their_dates() {
        val event = Event(id = 0, name = "Lunar", originalDate = LocalDate.of(1934, 9, 28), calendar = "chinese")
        val dates = listOf(LocalDate.of(2026, 9, 30), LocalDate.of(2027, 9, 20))
        val lines = ics(event, lunar = { dates }).split("\r\n")
        assertTrue("DTSTART;VALUE=DATE:20260930" in lines)
        assertTrue("RDATE;VALUE=DATE:20270920" in lines)
        assertTrue(lines.none { it.startsWith("RRULE") })
        // The original date still comes back
        assertEquals(LocalDate.of(1934, 9, 28), icsToEvents(lines.joinToString("\r\n")).single().originalDate)
    }

    @Test
    fun other_calendars_become_events_without_a_year() {
        val foreign = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:Team\\, dinner\r\n" +
            "DTSTART:20261015T190000Z\r\nEND:VEVENT\r\nBEGIN:VEVENT\r\nSUMMARY:No date\r\nEND:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        val event = icsToEvents(foreign).single()
        assertEquals("Team, dinner", event.name)
        assertEquals(LocalDate.of(2026, 10, 15), event.originalDate)
        assertEquals(EventCode.OTHER.name, event.type)
        assertEquals(false, event.yearMatter)
    }
}
