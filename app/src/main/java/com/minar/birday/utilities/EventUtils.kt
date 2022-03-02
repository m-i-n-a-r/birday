package com.minar.birday.utilities

import android.content.Context
import com.minar.birday.R
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.model.EventType
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*


// Transform an event result in a simple event
fun resultToEvent(eventResult: EventResult) = Event(
    id = eventResult.id,
    name = eventResult.name,
    surname = eventResult.surname,
    favorite = eventResult.favorite,
    originalDate = eventResult.originalDate,
    yearMatter = eventResult.yearMatter,
    notes = eventResult.notes,
    image = eventResult.image
)

// Check if an event is a birthday
fun isBirthday(event: EventResult): Boolean =
    event.type == EventCode.BIRTHDAY.toString()

// Check if an event is a anniversary
fun isAnniversary(event: EventResult): Boolean =
    event.type == EventCode.ANNIVERSARY.toString()

// Check if an event is a death anniversary
fun isDeathAnniversary(event: EventResult): Boolean =
    event.type == EventCode.DEATH.toString()

// Check if an event is a name day
fun isNameDay(event: EventResult): Boolean =
    event.type == EventCode.NAME_DAY.toString()

// Check if an event is "other"
fun isOther(event: EventResult): Boolean =
    event.type == EventCode.OTHER.toString()

// Properly format the next date for widget and next event card
fun nextDateFormatted(event: EventResult, formatter: DateTimeFormatter, context: Context): String {
    val daysRemaining = getRemainingDays(event.nextDate!!)
    return event.nextDate.format(formatter) + ". " + formatDaysRemaining(daysRemaining, context)
}

// Return the remaining days or a string
fun formatDaysRemaining(daysRemaining: Int, context: Context): String {
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

// Format the name considering the preference and the surname (which could be empty)
fun formatName(event: EventResult, surnameFirst: Boolean): String {
    return if (event.surname.isNullOrBlank()) event.name
    else {
        if (!surnameFirst) event.name + " " + event.surname
        else event.surname + " " + event.name
    }
}

// Get the reduced date for an event, i.e. the month and day date, unsupported natively
fun getReducedDate(date: LocalDate) =
    date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
            ", " + date.dayOfMonth.toString()

// Get the years also considering the possible corner cases
fun getYears(eventResult: EventResult): Int {
    var years = -2
    if (eventResult.yearMatter!!) years =
        eventResult.nextDate!!.year - eventResult.originalDate.year - 1
    return if (years == -1) 0 else years
}

// Get the months of the years. Useful for babies
fun getYearsMonths(date: LocalDate) = Period.between(date, LocalDate.now()).months

// Get the next years also considering the possible corner cases
fun getNextYears(eventResult: EventResult) = getYears(eventResult) + 1

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
    } catch (e: Exception) {
        context.getString(R.string.unknown)
    }
}