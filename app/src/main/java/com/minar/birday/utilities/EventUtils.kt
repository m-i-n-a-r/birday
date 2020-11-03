package com.minar.birday.utilities

import android.content.Context
import com.minar.birday.R
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
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

// Properly format the next date for widget and next event card
fun nextDateFormatted(event: EventResult, formatter: DateTimeFormatter, context: Context): String {
    val daysRemaining = getRemainingDays(event.nextDate!!)
    return event.nextDate.format(formatter) + ". " + daysRemaining(daysRemaining, context)
}

// Return the remaining days or a string
fun daysRemaining(daysRemaining: Int, context: Context): String {
    return when (daysRemaining) {
        // The -1 case should never happen
        -1 -> context.getString(R.string.yesterday)
        0 -> context.getString(R.string.today)
        1 -> context.getString(R.string.tomorrow)
        else -> context.resources.getQuantityString(R.plurals.days_left, daysRemaining, daysRemaining)
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

// Get the age also considering the possible corner cases
fun getAge(eventResult: EventResult): Int {
    var age = -2
    if (eventResult.yearMatter!!) age = eventResult.nextDate!!.year - eventResult.originalDate.year - 1
    return if (age == -1) 0 else age
}

// Get the months of the age. Useful for babies
fun getAgeMonths(date: LocalDate) = Period.between(date, LocalDate.now()).months

// Get the next age also considering the possible corner cases
fun getNextAge(eventResult: EventResult) = getAge(eventResult) + 1

// Get the decade of birth
fun getDecade(originalDate: LocalDate) = ((originalDate.year.toDouble() / 10).toInt() * 10).toString()

// Get the age range, in decades
fun getAgeRange(originalDate: LocalDate) = (((LocalDate.now().year - originalDate.year).toDouble() / 10).toInt() * 10).toString()

// Get the days remaining before an event from today
fun getRemainingDays(nextDate: LocalDate) = ChronoUnit.DAYS.between(LocalDate.now(), nextDate).toInt()