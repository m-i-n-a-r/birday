package com.minar.birday.utilities

import android.content.Context
import android.text.format.DateFormat
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.model.EventType
import com.minar.birday.persistence.LocalDateTypeConverter
import java.text.NumberFormat
import java.time.LocalDate
import java.time.MonthDay
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// Event related constants
const val START_YEAR = 0
const val COLUMN_TYPE = "type"
const val COLUMN_NAME = "name"
const val COLUMN_SURNAME = "surname"
const val COLUMN_DATE = "date"
const val COLUMN_YEAR_MATTER = "yearMatter"
const val COLUMN_NOTES = "notes"

// Transform an event result in a simple event
fun resultToEvent(eventResult: EventResult) = Event(
    id = eventResult.id,
    type = eventResult.type,
    name = eventResult.name,
    surname = eventResult.surname,
    favorite = eventResult.favorite,
    originalDate = eventResult.originalDate,
    yearMatter = eventResult.yearMatter,
    notes = eventResult.notes,
    image = eventResult.image,
    calendar = eventResult.calendar
)

// Transform an event in a result event
fun eventToResult(event: Event) = EventResult(
    id = event.id,
    type = event.type,
    name = event.name,
    surname = event.surname,
    favorite = event.favorite,
    originalDate = event.originalDate,
    nextDate = getNextDate(event.originalDate),
    yearMatter = event.yearMatter,
    notes = event.notes,
    image = event.image,
    calendar = event.calendar
)

// Simply returns the next date for a given date
fun getNextDate(date: LocalDate): LocalDate {
    val now = LocalDate.now()
    val nextDate = date.withYear(now.year)
    if (nextDate.isBefore(now)) nextDate.plusYears(1)
    return nextDate
}

// Destroy any illegal character and length in the fields, add missing fields if possible
fun normalizeEvent(event: Event): Event {
    // The id is automatically fixed in Room
    var fixedType = event.type
    if (isUnknownType(event.type?.uppercase()))
        fixedType = EventCode.OTHER.name

    // No restrictions on special characters for now, notes length is hardcoded to avoid using ctx
    return Event(
        id = 0,
        name = event.name.substring(IntRange(0, 30.coerceAtMost(event.name.length) - 1)),
        surname = event.surname?.substring(IntRange(0, 30.coerceAtMost(event.surname.length) - 1)),
        favorite = false,
        notes = event.notes?.substring(IntRange(0, 500.coerceAtMost(event.notes.length) - 1)),
        originalDate = event.originalDate,
        yearMatter = event.yearMatter,
        type = fixedType
    )
}

// Check if an event is a birthday
fun isBirthday(event: EventResult): Boolean =
    event.type == EventCode.BIRTHDAY.name

// Check if an event is a anniversary
fun isAnniversary(event: EventResult): Boolean =
    event.type == EventCode.ANNIVERSARY.name

// Check if an event is a death anniversary
fun isDeathAnniversary(event: EventResult): Boolean =
    event.type == EventCode.DEATH.name

// Check if an event is a name day
fun isNameDay(event: EventResult): Boolean =
    event.type == EventCode.NAME_DAY.name

// Check if an event is "other"
fun isOther(event: EventResult): Boolean =
    event.type == EventCode.OTHER.name

// Check if a given type, in string form, is unknown
fun isUnknownType(type: String?): Boolean {
    if (type.isNullOrBlank()) return true
    return !EventCode.entries.map { it.name }.contains(type)
}

// Properly format the next date for widget and next event card
fun nextDateFormatted(event: EventResult, formatter: DateTimeFormatter, context: Context): String {
    val daysRemaining = getRemainingDays(event.nextDate!!)
    return event.nextDate.format(formatter) + ". " + formatDaysRemaining(daysRemaining, context)
}

// Return the remaining days, properly formatted, including "yesterday" case
fun formatDaysRemaining(daysRemaining: Int, context: Context): String {
    // Special case: the event was yesterday
    if (daysRemaining > 363) {
        val previousOccurrence = LocalDate.now().plusDays(daysRemaining.toLong()).minusYears(1L)
        val wasYesterday = LocalDate.now().toEpochDay().minus(previousOccurrence.toEpochDay()) == 1L
        if (wasYesterday) return context.getString(R.string.yesterday)
    }
    return when (daysRemaining) {
        // The -1 case should never happen
        -1 -> context.getString(R.string.yesterday)
        0 -> context.getString(R.string.today)
        1 -> context.getString(R.string.tomorrow)
        else -> context.resources.getQuantityString(
            R.plurals.days_left,
            daysRemaining,
            daysRemaining
        )
    }
}

// Given an ordered series of events, remove the upcoming events or return them
fun removeOrGetUpcomingEvents(
    events: List<EventResult>,
    returnUpcoming: Boolean = false,
    onlyFavorites: Boolean = false
): List<EventResult> {
    val upcomingResult: MutableList<EventResult> = events.toMutableList()
    // Always exclude ignored events TODO test
    upcomingResult.removeIf { it.favorite == null }
    if (onlyFavorites)
        upcomingResult.removeIf { it.favorite == false }
    if (returnUpcoming) {
        upcomingResult.removeIf {
            it.nextDate!! != upcomingResult[0].nextDate
        }
    } else {
        upcomingResult.removeIf {
            it.nextDate!! == upcomingResult[0].nextDate
        }
    }
    return upcomingResult
}

// Given a series of events, format them considering the yearMatters parameter and the number
fun formatEventList(
    events: List<EventResult>,
    surnameFirst: Boolean,
    context: Context,
    showSurnames: Boolean = true,
    inCurrentYear: Boolean = false,
): String {
    var formattedEventList = ""
    if (events.isEmpty()) formattedEventList = context.getString(R.string.no_next_event)
    else events.takeWhile { events.indexOf(it) <= 3 }.forEach {
        // Years. They're not used in the string if the year doesn't matter
        val years = if (inCurrentYear && it.originalDate.withYear(LocalDate.now().year)
                .isBefore(LocalDate.now())
        ) (getNextYears(it) - 1).coerceAtLeast(0) else getNextYears(it)
        // Only the data of the first 3 events are displayed
        if (events.indexOf(it) in 0..2) {
            // If the event is not the first, add an extra comma
            if (events.indexOf(it) != 0)
                formattedEventList += ", "

            // Show the last name, if any, if there's only one event
            formattedEventList +=
                if (events.size == 1 && showSurnames) formatName(it, surnameFirst)
                else it.name

            // Show event type if different from birthday
            if (it.type != EventCode.BIRTHDAY.name)
                formattedEventList += " (${getStringForTypeCodename(context, it.type!!)})"
            // If the year is considered, display it. Else only display the name
            if (it.yearMatter!!) formattedEventList += ", " +
                    context.resources.getQuantityString(
                        R.plurals.years,
                        years,
                        years
                    )
        }
        // If more than 3 events, just let the user know other events are in the list
        if (events.indexOf(it) == 3)
            formattedEventList += ", ${context.getString(R.string.event_others)}"
    }
    return formattedEventList
}

// Format the name considering the preference and the surname (which could be empty)
fun formatName(event: EventResult, surnameFirst: Boolean): String {
    return if (event.surname.isNullOrBlank()) event.name
    else {
        if (!surnameFirst) "${event.name} ${event.surname}"
        else "${event.surname} ${event.name}"
    }
}

// Get the reduced date for an event, i.e. the month and day date, unsupported natively
fun getReducedDate(date: LocalDate) = forceMonthDayFormat(date, FormatStyle.FULL)

// Get the years also considering the possible corner cases
fun getYears(eventResult: EventResult): Int {
    var years = -2
    if (eventResult.yearMatter!!) years =
        eventResult.nextDate!!.year - eventResult.originalDate.year - 1
    return if (years <= -1) 0 else years
}

// Get the months of the years. Useful for babies
fun getYearsMonths(date: LocalDate) = Period.between(date, LocalDate.now()).months

// Get the next years also considering the possible corner cases
fun getNextYears(eventResult: EventResult): Int {
    var years = -2
    if (eventResult.yearMatter!!) years =
        eventResult.nextDate!!.year - eventResult.originalDate.year
    return if (years <= -1 && eventResult.yearMatter) 0 else years
}

// A round number of days lived, celebrated like a birthday
const val DAYS_MILESTONE = 1000L

// The whole days lived feature (counter, celebrations, notifications) is off unless opted in
fun daysMilestonesEnabled(context: Context) = PreferenceManager
    .getDefaultSharedPreferences(context)
    .getBoolean("days_milestones", false)

// Days since the birth. Only a birthday with a known year, already happened, has an answer
fun getDaysLived(eventResult: EventResult, on: LocalDate = LocalDate.now()): Long? {
    if (eventResult.type != EventCode.BIRTHDAY.name || eventResult.yearMatter != true) return null
    val days = ChronoUnit.DAYS.between(eventResult.originalDate, on)
    return if (days >= 0) days else null
}

// Whether the given day is a milestone for this person (the day of birth itself is not)
fun isDaysMilestone(eventResult: EventResult, on: LocalDate = LocalDate.now()): Boolean {
    val days = getDaysLived(eventResult, on) ?: return false
    return days > 0 && days % DAYS_MILESTONE == 0L
}

// The days lived with the digits grouped as the locale wants them, "10,000" or "10.000"
fun formatDaysLived(days: Long): String = NumberFormat.getIntegerInstance().format(days)

// Same shape as formatEventList, with the milestone in place of the age
fun formatMilestoneList(
    events: List<EventResult>,
    surnameFirst: Boolean,
    context: Context,
    on: LocalDate = LocalDate.now(),
): String {
    var formattedEventList = ""
    events.take(3).forEachIndexed { index, event ->
        if (index != 0) formattedEventList += ", "
        formattedEventList +=
            if (events.size == 1) formatName(event, surnameFirst) else event.name
        val days = getDaysLived(event, on) ?: return@forEachIndexed
        formattedEventList += ", " + context.resources.getQuantityString(
            R.plurals.days_lived_count,
            days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            formatDaysLived(days)
        )
    }
    if (events.size > 3) formattedEventList += ", ${context.getString(R.string.event_others)}"
    return formattedEventList
}

// Keys of the card at the top of the settings, where the user tells who they are
const val PREF_USER_NAME = "user_name"
const val PREF_USER_BIRTHDAY = "user_birthday"

// The user's own name and birthday. Both are needed to celebrate, one without the other is nothing
fun getUserBirthday(context: Context): Pair<String, LocalDate>? {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val name = prefs.getString(PREF_USER_NAME, null)?.trim().orEmpty()
    val birthday = prefs.getString(PREF_USER_BIRTHDAY, null)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    return if (name.isNotEmpty() && birthday != null) name to birthday else null
}

// Whether a day is the user's birthday. February 29 follows the same rule as the events do
fun isUserBirthday(birthday: LocalDate, on: LocalDate = LocalDate.now()): Boolean {
    if (birthday.monthValue == 2 && birthday.dayOfMonth == 29 && !on.isLeapYear) {
        val substitute =
            if (LocalDateTypeConverter.useFebruary28) MonthDay.of(2, 28) else MonthDay.of(3, 1)
        return MonthDay.from(on) == substitute
    }
    return MonthDay.from(birthday) == MonthDay.from(on)
}

// The next day the user is celebrated, today included. There's always one within a year
fun nextUserBirthday(birthday: LocalDate, from: LocalDate = LocalDate.now()): LocalDate =
    generateSequence(from) { it.plusDays(1) }.take(367).first { isUserBirthday(birthday, it) }

// Unbirthdays (#29) are off unless opted in
fun unbirthdaysEnabled(context: Context) = PreferenceManager
    .getDefaultSharedPreferences(context)
    .getBoolean("unbirthdays", false)

// A 13th falls on each day of the week within 14 months, but a 31st can take years
private const val UNBIRTHDAY_SEARCH_MONTHS = 120L

// The next day, today included, with the same day of the month and day of the week as the birth,
// like Friday the 13th for someone born on a Friday the 13th. It can also be the birthday itself
fun getNextUnbirthday(eventResult: EventResult, from: LocalDate = LocalDate.now()): LocalDate? {
    if (eventResult.type != EventCode.BIRTHDAY.name || eventResult.yearMatter != true) return null
    val birth = eventResult.originalDate
    if (birth.isAfter(from)) return null
    val firstMonth = YearMonth.from(from)
    for (offset in 0 until UNBIRTHDAY_SEARCH_MONTHS) {
        val month = firstMonth.plusMonths(offset)
        if (!month.isValidDay(birth.dayOfMonth)) continue
        val candidate = month.atDay(birth.dayOfMonth)
        if (!candidate.isBefore(from) && candidate.dayOfWeek == birth.dayOfWeek) return candidate
    }
    return null
}

// Get the decade of birth
fun getDecade(originalDate: LocalDate) =
    ((originalDate.year.toDouble() / 10).toInt() * 10).toString()

// Get the age range, in decades, should be used only for birthdays
fun getAgeRange(originalDate: LocalDate) =
    (((LocalDate.now().year - originalDate.year).toDouble() / 10).toInt() * 10).toString()

// Get the days remaining before an event from today
fun getRemainingDays(nextDate: LocalDate) =
    ChronoUnit.DAYS.between(LocalDate.now(), nextDate).toInt()

// Return the resolved representation of each event type
fun getAvailableTypes(context: Context): List<EventType> {
    return listOf(
        EventType(EventCode.BIRTHDAY, context.getString(R.string.birthday)),
        EventType(EventCode.ANNIVERSARY, context.getString(R.string.anniversary)),
        EventType(EventCode.DEATH, context.getString(R.string.death_anniversary)),
        EventType(EventCode.NAME_DAY, context.getString(R.string.name_day)),
        EventType(EventCode.OTHER, context.getString(R.string.other)),
    )
}

// Given a string, returns the corresponding translated event type, if any
fun getStringForTypeCodename(context: Context, codename: String): String {
    return try {
        when (EventCode.valueOf(codename.uppercase())) {
            EventCode.BIRTHDAY -> context.getString(R.string.birthday)
            EventCode.ANNIVERSARY -> context.getString(R.string.anniversary)
            EventCode.DEATH -> context.getString(R.string.death_anniversary)
            EventCode.NAME_DAY -> context.getString(R.string.name_day)
            EventCode.OTHER -> context.getString(R.string.other)
        }
    } catch (_: Exception) {
        context.getString(R.string.unknown)
    }
}

// Format a normal LocalDate in a year-less format, respecting the locale conventions
fun forceMonthDayFormat(date: LocalDate, style: FormatStyle = FormatStyle.MEDIUM): String {
    // The skeleton is resolved to the correct localized month-day pattern (e.g. "d. MMMM" in German)
    val skeleton = when (style) {
        FormatStyle.SHORT -> "Md"
        FormatStyle.MEDIUM -> "MMMd"
        else -> "MMMMd"
    }
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

// Format a text preview for a given event, useful for the share event and import event dialog scenarios
fun formatTextPreview(
    event: EventResult,
    context: Context,
    surnameFirst: Boolean = false,
    multiline: Boolean = true
): String {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
    var typeEmoji = String(Character.toChars(0x1F973))
    when (event.type) {
        EventCode.ANNIVERSARY.name -> typeEmoji = String(Character.toChars(0x1F495))
        EventCode.DEATH.name -> typeEmoji = String(Character.toChars(0x1FAA6))
        EventCode.NAME_DAY.name -> typeEmoji = String(Character.toChars(0x1F607))
        EventCode.OTHER.name -> typeEmoji = String(Character.toChars(0x1F7E2))
    }
    val eventInformation =
        if (multiline)
            String(Character.toChars(0x1F388)) + "  " +
                    context.getString(R.string.notification_title) +
                    "\n" + typeEmoji + "  " +
                    formatName(event, surnameFirst) +
                    " (" + getStringForTypeCodename(context, event.type!!) +
                    ")\n" + String(Character.toChars(0x1F56F)) + "  " +
                    event.nextDate!!.format(formatter) +
                    // Add a fourth line with the original date, if the year matters
                    if (event.yearMatter!!)
                        "\n" + String(Character.toChars(0x1F4C5)) + "  " +
                                event.originalDate.format(formatter)
                    else ""
        else "$typeEmoji ${
            formatName(
                event,
                surnameFirst
            )
        }\n${event.originalDate.format(formatter)}"
    return eventInformation
}