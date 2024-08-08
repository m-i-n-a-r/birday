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

// Event related constants
const val START_YEAR = 0
const val COLUMN_TYPE = "type"
const val COLUMN_NAME = "name"
const val COLUMN_SURNAME = "surname"
const val COLUMN_DATE = "date"
const val COLUMN_YEAR_MATTER = "yearMatter"
const val COLUMN_NOTES = "notes"

//vehicle insurance add event
const val COLUMN_VEHICLE_MANUFACTURER_NAME = "manufacturerName"
const val COLUMN_VEHICLE_MANUFACTURER_NAME1 = "manufacturerName1"
const val COLUMN_VEHICLE_MANUFACTURER_NAME2 = "manufacturerName2"
const val COLUMN_VEHICLE_MANUFACTURER_NAME3 = "manufacturerName3"

const val COLUMN_VEHICLE_MODELNAME = "modelName"
const val COLUMN_VEHICLE_MODELNAME1 = "modelName1"
const val COLUMN_VEHICLE_MODELNAME2 = "modelName2"
const val COLUMN_VEHICLE_MODELNAME3 = "modelName3"

const val COLUMN_VEHICLE_INSURANCEPROVIDER = "insuranceProvider"

//vehicle insurance renewal add event
const val COLUMN_INPUT1 = "input1"
const val COLUMN_INPUT2 = "input2"
const val COLUMN_INPUT3 = "input3"
const val COLUMN_INPUT4 = "input4"
const val COLUMN_INPUT5 = "input5"
const val COLUMN_INPUT6 = "input6"
const val COLUMN_INPUT7 = "input7"
const val COLUMN_INPUT8 = "input8"
const val COLUMN_INPUT9 = "input9"
const val COLUMN_INPUT10 = "input10"

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
    //vehicle insurance add event
    manufacturerName = eventResult.manufacturerName!!,
    manufacturerName1 = eventResult.manufacturerName1!!,
    manufacturerName2 = eventResult.manufacturerName2!!,
    manufacturerName3 = eventResult.manufacturerName3!!,

    modelName = eventResult.modelName!!,
    modelName1 = eventResult.modelName1!!,
    modelName2 = eventResult.modelName2!!,
    modelName3 = eventResult.modelName3!!,

    //vehicle insurance renewal add event
    input1 = eventResult.input1!!,
    input2 = eventResult.input2!!,
    input3 = eventResult.input3!!,
    input4 = eventResult.input4!!,
    input5 = eventResult.input5!!,
    input6 = eventResult.input6!!,
    input7 = eventResult.input7!!,
    input8 = eventResult.input8!!,
    input9 = eventResult.input9!!,
    input10 = eventResult.input10!!,

    insuranceProvider = eventResult.insuranceProvider!!
)

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
        //vehicle insurance add event
        manufacturerName = event.manufacturerName?.substring(IntRange(0, 30.coerceAtMost(event.manufacturerName.length) - 1)),
        manufacturerName1 = event.manufacturerName1?.substring(IntRange(0, 30.coerceAtMost(event.manufacturerName1.length) - 1)),
        manufacturerName2 = event.manufacturerName2?.substring(IntRange(0, 30.coerceAtMost(event.manufacturerName2.length) - 1)),
        manufacturerName3 = event.manufacturerName3?.substring(IntRange(0, 30.coerceAtMost(event.manufacturerName3.length) - 1)),

        modelName = event.modelName?.substring(IntRange(0, 30.coerceAtMost(event.modelName.length) - 1)),
        modelName1 = event.modelName1?.substring(IntRange(0, 30.coerceAtMost(event.modelName1.length) - 1)),
        modelName2 = event.modelName2?.substring(IntRange(0, 30.coerceAtMost(event.modelName2.length) - 1)),
        modelName3 = event.modelName3?.substring(IntRange(0, 30.coerceAtMost(event.modelName3.length) - 1)),

        insuranceProvider = event.insuranceProvider?.substring(IntRange(0, 30.coerceAtMost(event.insuranceProvider.length) - 1)),
        //vehicle insurance renewal add event
        input1 = event.input1.substring(IntRange(0, 30.coerceAtMost(event.input1.length) - 1)),
        input2 = event.input2.substring(IntRange(0, 30.coerceAtMost(event.input2.length) - 1)),
        input3 = event.input3.substring(IntRange(0, 30.coerceAtMost(event.input3.length) - 1)),
        input4 = event.input4.substring(IntRange(0, 30.coerceAtMost(event.input4.length) - 1)),
        input5 = event.input5.substring(IntRange(0, 30.coerceAtMost(event.input5.length) - 1)),
        input6 = event.input6.substring(IntRange(0, 30.coerceAtMost(event.input6.length) - 1)),
        input7 = event.input7.substring(IntRange(0, 30.coerceAtMost(event.input7.length) - 1)),
        input8 = event.input8.substring(IntRange(0, 30.coerceAtMost(event.input8.length) - 1)),
        input9 = event.input9.substring(IntRange(0, 30.coerceAtMost(event.input9.length) - 1)),
        input10 = event.input10.substring(IntRange(0, 30.coerceAtMost(event.input10.length) - 1)),

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

// Check if an event is a vehicle insurance
fun isVehicleInsurance(event: EventResult): Boolean =
    event.type == EventCode.VEHICLE_INSURANCE.name

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
    returnUpcoming: Boolean = false
): List<EventResult> {
    val upcomingResult: MutableList<EventResult> = events.toMutableList()
    if (returnUpcoming) {
        upcomingResult.removeIf {
            it.nextDate!! != events[0].nextDate
        }
    } else {
        upcomingResult.removeIf {
            it.nextDate!! == events[0].nextDate
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
fun getReducedDate(date: LocalDate) =
    date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
            ", " + date.dayOfMonth.toString()

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
        EventType(EventCode.VEHICLE_INSURANCE, context.getString(R.string.vehicle_insurance)),
        EventType(EventCode.VEHICLE_INSURANCE_RENEWAL, context.getString(R.string.vehicle_insurance_renewal)),
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
            EventCode.VEHICLE_INSURANCE -> context.getString(R.string.vehicle_insurance)
            EventCode.VEHICLE_INSURANCE_RENEWAL -> context.getString(R.string.vehicle_insurance_renewal)
            EventCode.OTHER -> context.getString(R.string.other)
        }
    } catch (e: Exception) {
        context.getString(R.string.unknown)
    }
}

