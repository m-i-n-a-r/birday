package com.minar.birday.utilities

import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64


// iCalendar (RFC 5545) for the events: a file any calendar app understands, as a yearly all day
// event, and that Birday reads back exactly thanks to its own X-BIRDAY properties

const val ICS_MIME_TYPE = "text/calendar"

private const val PROPERTY_NAME = "X-BIRDAY-NAME"
private const val PROPERTY_SURNAME = "X-BIRDAY-SURNAME"
private const val PROPERTY_TYPE = "X-BIRDAY-TYPE"
private const val PROPERTY_DATE = "X-BIRDAY-DATE"
private const val PROPERTY_YEAR = "X-BIRDAY-YEAR"
private const val PROPERTY_CALENDAR = "X-BIRDAY-CALENDAR"
// An inline attachment, as the standard allows: calendar apps ignore it, Birday takes it back as
// the picture of the event
private const val IMAGE_ATTACHMENT = "ATTACH-IMAGE"

// Lines are folded past 75 octets, and the limits of the fields are the ones of the insert sheet
private const val MAX_LINE_OCTETS = 75
private const val MAX_NAME_LENGTH = 30
private const val MAX_NOTES_LENGTH = 500

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
private val STAMP_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

/**
 * The events as an iCalendar file.
 * @param summary The title shown by calendar apps, as "Mario Rossi (Birthday)"
 * @param lunarDates For an event following an alternative calendar, its next dates, since most
 * calendar apps can't follow one. Null for a Gregorian event
 * @param february28 Where a February 29 falls in common years, as in the settings
 * @param includeImages Whether the pictures travel too: fine for an event, heavy for all of them
 */
fun eventsToIcs(
    events: List<Event>,
    summary: (Event) -> String,
    lunarDates: (Event) -> List<LocalDate>? = { null },
    february28: Boolean = false,
    includeNotes: Boolean = true,
    includeImages: Boolean = false,
    now: Instant = Instant.now(),
): String {
    val lines = mutableListOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//minar//Birday//EN",
        "CALSCALE:GREGORIAN",
    )
    for (event in events) {
        lines += "BEGIN:VEVENT"
        lines += "UID:${eventUid(event)}"
        lines += "DTSTAMP:${STAMP_FORMAT.format(now)}"
        val dates = lunarDates(event)
        if (!dates.isNullOrEmpty()) {
            lines += "DTSTART;VALUE=DATE:${dates.first().format(DATE_FORMAT)}"
            if (dates.size > 1)
                lines += "RDATE;VALUE=DATE:" + dates.drop(1).joinToString(",") { it.format(DATE_FORMAT) }
        } else {
            lines += "DTSTART;VALUE=DATE:${event.originalDate.format(DATE_FORMAT)}"
            lines += "RRULE:" + yearlyRule(event.originalDate, february28)
        }
        lines += "SUMMARY:${escape(summary(event))}"
        if (includeNotes && !event.notes.isNullOrBlank())
            lines += "DESCRIPTION:${escape(event.notes)}"
        lines += "TRANSP:TRANSPARENT"
        lines += "$PROPERTY_NAME:${escape(event.name)}"
        if (!event.surname.isNullOrBlank()) lines += "$PROPERTY_SURNAME:${escape(event.surname)}"
        lines += "$PROPERTY_TYPE:${event.type ?: EventCode.OTHER.name}"
        lines += "$PROPERTY_DATE:${event.originalDate.format(DATE_FORMAT)}"
        lines += "$PROPERTY_YEAR:${if (event.yearMatter == false) "FALSE" else "TRUE"}"
        if (event.calendar != null) lines += "$PROPERTY_CALENDAR:${event.calendar}"
        if (includeImages && event.image != null && event.image.isNotEmpty())
            lines += "ATTACH;FMTTYPE=${imageType(event.image)};ENCODING=BASE64;VALUE=BINARY:" +
                Base64.getEncoder().encodeToString(event.image)
        lines += "END:VEVENT"
    }
    lines += "END:VCALENDAR"
    return lines.joinToString("") { fold(it) + "\r\n" }
}

// The pictures are stored as JPEG, but a PNG says so too
private fun imageType(image: ByteArray): String =
    if (image.size > 1 && image[0] == 0x89.toByte() && image[1] == 0x50.toByte()) "image/png"
    else "image/jpeg"

// The same event exported twice keeps its identity, so a calendar updates it instead of doubling it
private fun eventUid(event: Event): String {
    val identity = "${event.name}|${event.surname.orEmpty()}|${event.type}|${event.originalDate}"
    val hash = Integer.toHexString(identity.hashCode())
    return "birday-${event.originalDate.format(DATE_FORMAT)}-$hash@minar.birday"
}

// Every year on that day. February 29 has no day in common years: the last day of February, or
// the 60th of the year, which is March 1 in common years and February 29 in leap ones
private fun yearlyRule(date: LocalDate, february28: Boolean): String = when {
    date.monthValue != 2 || date.dayOfMonth != 29 -> "FREQ=YEARLY"
    february28 -> "FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=-1"
    else -> "FREQ=YEARLY;BYYEARDAY=60"
}

private fun escape(text: String): String = text
    .replace("\\", "\\\\")
    .replace(";", "\\;")
    .replace(",", "\\,")
    .replace("\r\n", "\\n")
    .replace("\n", "\\n")

private fun unescape(text: String): String {
    val result = StringBuilder()
    var index = 0
    while (index < text.length) {
        val char = text[index]
        if (char == '\\' && index + 1 < text.length) {
            val next = text[index + 1]
            result.append(if (next == 'n' || next == 'N') '\n' else next)
            index += 2
        } else {
            result.append(char)
            index++
        }
    }
    return result.toString()
}

// Long lines continue on the next one after a space, never splitting a character
private fun fold(line: String): String {
    if (line.toByteArray(Charsets.UTF_8).size <= MAX_LINE_OCTETS) return line
    val result = StringBuilder()
    var octets = 0
    var index = 0
    // The first line has the whole limit, the next ones lose an octet to the leading space
    var limit = MAX_LINE_OCTETS
    while (index < line.length) {
        val codePoint = line.codePointAt(index)
        val chars = Character.charCount(codePoint)
        val size = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
        if (octets + size > limit) {
            result.append("\r\n ")
            octets = 0
            limit = MAX_LINE_OCTETS - 1
        }
        result.append(line, index, index + chars)
        octets += size
        index += chars
    }
    return result.toString()
}

/**
 * The events in an iCalendar file. Those exported by Birday come back as they were, any other
 * event becomes one of type other, from its title and first day, without a year: a calendar
 * series rarely starts from a birth. Anything unreadable is skipped
 */
fun icsToEvents(text: String): List<Event> {
    // Unfold first: a line starting with a space or a tab continues the one before
    val lines = text.replace("\r\n", "\n").replace("\r", "\n")
        .replace(Regex("\n[ \t]"), "")
        .split("\n")
    val events = mutableListOf<Event>()
    var properties: MutableMap<String, String>? = null
    for (line in lines) {
        when {
            line.equals("BEGIN:VEVENT", ignoreCase = true) -> properties = mutableMapOf()
            line.equals("END:VEVENT", ignoreCase = true) -> {
                properties?.let { eventFrom(it) }?.let { events += it }
                properties = null
            }

            properties != null -> {
                val colon = line.indexOf(':')
                if (colon <= 0) continue
                // The parameters, as ;VALUE=DATE, don't matter here, but for an inline picture
                val head = line.substring(0, colon).uppercase()
                val name = head.substringBefore(';')
                val isImage = name == "ATTACH" && "ENCODING=BASE64" in head && "FMTTYPE=IMAGE/" in head
                properties.putIfAbsent(if (isImage) IMAGE_ATTACHMENT else name, line.substring(colon + 1))
            }
        }
    }
    return events
}

private fun eventFrom(properties: Map<String, String>): Event? {
    val image = properties[IMAGE_ATTACHMENT]?.let {
        runCatching { Base64.getDecoder().decode(it.trim()) }.getOrNull()
    }
    val birdayDate = properties[PROPERTY_DATE]?.let { parseDate(it) }
    val birdayName = properties[PROPERTY_NAME]?.let { unescape(it).trim() }
    if (birdayDate != null && !birdayName.isNullOrBlank()) {
        val type = properties[PROPERTY_TYPE]?.uppercase()
        return Event(
            id = 0,
            type = if (isUnknownType(type)) EventCode.OTHER.name else type,
            name = birdayName.take(MAX_NAME_LENGTH),
            surname = properties[PROPERTY_SURNAME]?.let { unescape(it).trim().take(MAX_NAME_LENGTH) }
                .orEmpty(),
            originalDate = birdayDate,
            yearMatter = !properties[PROPERTY_YEAR].equals("FALSE", ignoreCase = true),
            notes = properties["DESCRIPTION"]?.let { unescape(it).take(MAX_NOTES_LENGTH) }.orEmpty(),
            calendar = properties[PROPERTY_CALENDAR]?.let { EventCalendar.fromKey(it)?.key },
            image = image,
        )
    }
    val title = properties["SUMMARY"]?.let { unescape(it).trim() }
    val start = properties["DTSTART"]?.let { parseDate(it) }
    if (title.isNullOrBlank() || start == null) return null
    return Event(
        id = 0,
        type = EventCode.OTHER.name,
        name = title.take(MAX_NAME_LENGTH),
        originalDate = start,
        yearMatter = false,
        notes = properties["DESCRIPTION"]?.let { unescape(it).take(MAX_NOTES_LENGTH) }.orEmpty(),
        image = image,
    )
}

// A date, or the date part of a date and time
private fun parseDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim().take(8), DATE_FORMAT) }.getOrNull()
