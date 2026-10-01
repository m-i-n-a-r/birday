package com.minar.birday.utilities

import android.content.Context
import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.persistence.EventDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.min


// The calendars an event can recur by, besides the Gregorian one. Only lunar and lunisolar ones:
// a solar calendar puts a birthday on the same Gregorian day every year, give or take one, so
// offering it would only confuse. The key is both the stored value and the ICU calendar type
enum class EventCalendar(val key: String, @param:StringRes val title: Int) {
    CHINESE("chinese", R.string.calendar_chinese),
    DANGI("dangi", R.string.calendar_dangi),
    HEBREW("hebrew", R.string.calendar_hebrew),
    ISLAMIC("islamic-umalqura", R.string.calendar_islamic),
    ISLAMIC_CIVIL("islamic-civil", R.string.calendar_islamic_civil);

    companion object {
        fun fromKey(key: String?): EventCalendar? = entries.firstOrNull { it.key == key }
    }
}

// The alternative calendar chosen in the settings, if any. Null means the app works exactly as it
// always did: every event is Gregorian, whatever is stored in it
private const val PREF_ALTERNATIVE_CALENDAR = "alternative_calendar"

fun alternativeCalendar(context: Context): EventCalendar? = EventCalendar.fromKey(
    PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_ALTERNATIVE_CALENDAR, null)
)

// Hebrew months as ICU numbers them: Adar I only exists in leap years, Adar is the one of common
// years and Adar II of leap ones
private const val HEBREW_ADAR_1 = 5
private const val HEBREW_ADAR = 6

// A few years are always enough, the loop only guards against the impossible
private const val MAX_YEARS_AHEAD = 3

// Noon in UTC: no time zone can push the day to the one before or after
private fun LocalDate.toCalendarMillis() =
    atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * The next day, [from] included, a date comes back in the given calendar: same month and day of
 * that calendar, wherever they fall in the Gregorian year. A leap month missing that year becomes
 * the regular one, Adar I of a common Hebrew year becomes Adar, a day past the end of a shorter
 * month becomes its last day.
 * @param newCalendar Function to create a calendar of the given ICU type, in UTC
 */
fun <C : CalendarAdapter> nextOccurrenceGeneric(
    newCalendar: (type: String) -> C,
    date: LocalDate,
    calendar: EventCalendar,
    from: LocalDate,
): LocalDate {
    val original = newCalendar(calendar.key)
    original.setTimeInMillis(date.toCalendarMillis())
    val month = original.get(Calendar.MONTH)
    val day = original.get(Calendar.DAY_OF_MONTH)
    val leapMonth = original.get(Calendar.IS_LEAP_MONTH)

    val today = newCalendar(calendar.key)
    today.setTimeInMillis(from.toCalendarMillis())
    val firstYear = today.get(Calendar.EXTENDED_YEAR)
    for (year in firstYear..firstYear + MAX_YEARS_AHEAD) {
        val candidate = dateInYear(newCalendar(calendar.key), calendar, year, month, day, leapMonth)
        if (!candidate.isBefore(from)) return candidate
    }
    throw IllegalStateException("No occurrence of $date in the $calendar calendar")
}

// The Gregorian date of a month and day in the given year of the calendar
private fun dateInYear(
    target: CalendarAdapter,
    calendar: EventCalendar,
    year: Int,
    month: Int,
    day: Int,
    leapMonth: Int,
): LocalDate {
    fun setMonth(month: Int, leap: Int) {
        target.clear()
        target.set(Calendar.EXTENDED_YEAR, year)
        target.set(Calendar.MONTH, month)
        target.set(Calendar.IS_LEAP_MONTH, leap)
        target.set(Calendar.DAY_OF_MONTH, 1)
    }

    val targetMonth =
        if (calendar == EventCalendar.HEBREW && month == HEBREW_ADAR_1 && !isHebrewLeapYear(year))
            HEBREW_ADAR
        else month
    setMonth(targetMonth, leapMonth)
    // Reading the fields back tells whether that leap month exists in this year
    if (leapMonth == 1 && target.get(Calendar.IS_LEAP_MONTH) != 1) setMonth(targetMonth, 0)
    target.set(Calendar.DAY_OF_MONTH, min(day, target.getActualMaximum(Calendar.DAY_OF_MONTH)))
    return target.getTimeInMillis().toLocalDate()
}

// Seven leap years in every nineteen, the Metonic cycle
private fun isHebrewLeapYear(year: Int) = (7 * year + 1) % 19 < 7

// Android's own ICU, in UTC like the dates it is given
private fun androidCalendar(type: String) =
    AndroidCalendar(Calendar.getInstance(TimeZone.GMT_ZONE, ULocale("@calendar=$type")))

// The next occurrence of a date in the given calendar, [from] included
fun nextOccurrence(date: LocalDate, calendar: EventCalendar, from: LocalDate = LocalDate.now()) =
    nextOccurrenceGeneric(::androidCalendar, date, calendar, from)

// An alternative calendar moves a date around the Gregorian year, so its next date is computed here
// and stored for the queries. Turned off, every stored date is dropped: the queries fall back to
// the Gregorian date and the app is exactly what it was. Call it off the main thread
fun refreshCalendarDates(context: Context) {
    val eventDao = EventDatabase.getBirdayDatabase(context).eventDao()
    if (alternativeCalendar(context) == null) {
        eventDao.clearNextDateOverrides()
        return
    }
    for (event in eventDao.getAlternativeCalendarEvents()) {
        val calendar = EventCalendar.fromKey(event.calendar)
        // Without the year there's no day to convert: such an event stays Gregorian
        val nextDate =
            if (calendar == null || event.yearMatter != true) null
            else nextOccurrence(event.originalDate, calendar)
        if (nextDate != event.nextDateOverride) eventDao.setNextDateOverride(event.id, nextDate)
    }
}

// Languages with real names for the lunar months. Elsewhere ICU has none: older versions fall back
// to the Gregorian names, a lie, newer ones to codes like M08, so the month is a number there, the
// way lunar dates are usually written anyway
private val LUNAR_MONTH_NAME_LANGUAGES = setOf("zh", "ko", "ja", "vi")

// The day and month of a date in the given calendar, in the words of the current locale
fun formatInCalendar(date: LocalDate, calendar: EventCalendar): String {
    val icuCalendar = Calendar.getInstance(TimeZone.GMT_ZONE, ULocale("@calendar=${calendar.key}"))
    icuCalendar.timeInMillis = date.toCalendarMillis()
    val locale = Locale.getDefault()
    val lunar = calendar == EventCalendar.CHINESE || calendar == EventCalendar.DANGI
    val skeleton = if (lunar && locale.language !in LUNAR_MONTH_NAME_LANGUAGES) "Md" else "MMMMd"
    return DateFormat.getInstanceForSkeleton(icuCalendar, skeleton, locale).format(icuCalendar)
}
