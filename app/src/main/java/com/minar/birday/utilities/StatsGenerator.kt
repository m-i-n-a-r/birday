package com.minar.birday.utilities

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.minar.birday.R
import com.minar.birday.model.EventResult
import com.minar.birday.model.Stat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.math.truncate
import kotlin.random.Random

// How many times generateRandomStat() re-rolls before admitting that this event list cannot
// produce a single stat. Twelve branches, so a couple of dozen rolls is plenty
private const val MAX_RANDOM_STAT_ATTEMPTS = 24

// Generate a series of stats based on a list of events and focused on birthdays
class StatsGenerator(
    eventList: List<EventResult>,
    context: Context,
    private val astrologyDisabled: Boolean = false
) {
    private val events: List<EventResult> = eventList
    private val birthdays = filterBirthdays()
    private val anniversaries = filterAnniversaries()
    private val deathAnniversaries = filterDeathAnniversaries()
    private val nameDays = filterNameDays()
    private val others = filterOthers()
    private val applicationContext = context
    // The accent the highlighted values are painted with, resolved once against the themed context
    private val highlightColor = getThemeColor(R.attr.colorPrimary, context)

    // Generate a random stat choosing randomly between one of the available functions. The card it
    // ends up in wants one flat run of text, so the highlighting the stats sheet relies on is
    // dropped here on the way out
    fun generateRandomStat(): String {
        // Use a response string to re-execute the stats calculation if a stat cannot be computed correctly
        var response: CharSequence? = null
        val randomPerson = birthdays.randomOrNull() ?: return ""
        // Rolling again until something comes out is the whole point, the card is meant to show a
        // different stat every time it is opened. The cap only exists because a list where nothing
        // at all can be computed would otherwise spin here forever
        var attempts = 0
        while (response.isNullOrBlank() && attempts < MAX_RANDOM_STAT_ATTEMPTS) {
            attempts++
            response = when (Random.nextInt(0, 12)) {
                1 -> ageAverage()
                2 -> mostCommonMonth()
                3 -> mostCommonDecade()
                4 -> mostCommonAgeRange()
                5 -> specialAges()
                6 -> leapYearTotal()
                7 -> if (astrologyDisabled) dayOfWeek(randomPerson) else mostCommonZodiacSign()
                8 -> mostCommonDayOfWeek()
                9 -> dayOfWeek(randomPerson)
                10 -> if (astrologyDisabled) dayOfWeek(randomPerson) else zodiacSign(randomPerson)
                11 -> if (astrologyDisabled) dayOfWeek(randomPerson) else chineseSign(randomPerson)
                else -> ageAverage()
            }
        }
        return response?.toString() ?: ""
    }

    // Generate a summary of the cumulative stats
    fun generateFullStats(): List<Stat> = buildList {
        // Every stat below needs at least one birthday, only the type recap works without
        if (birthdays.isNotEmpty()) {
            addStat(R.drawable.ic_stats_24dp, ageAverage())
            addStat(R.drawable.ic_elderly_24dp, oldestPerson())
            addStat(R.drawable.ic_child_care_24dp, youngestPerson())
            addStat(R.drawable.ic_groups_24dp, mostCommonAgeRange())
            addStat(R.drawable.ic_event_repeat_24dp, mostCommonDayOfWeek())
            addStat(R.drawable.ic_history_24dp, mostCommonDecade())
            addStat(R.drawable.ic_date_black_24dp, mostCommonMonth())
            addStat(R.drawable.ic_event_available_24dp, leapYearTotal())
            // Only include astrology related stats if astrology is enabled
            if (!astrologyDisabled) {
                // The only icon here that is genuinely the subject of its own line: the winning
                // sign draws itself
                addStat(zodiacDrawable(mostCommonZodiacSignNumber()), mostCommonZodiacSign())
                addStat(R.drawable.ic_pets_24dp, mostCommonChineseSign())
            }
        }
        addStat(R.drawable.ic_event_type_black_24dp, eventTypesNumbers())
    }

    // A stat that could not be computed comes back blank, and a blank line is not worth a row
    private fun MutableList<Stat>.addStat(@DrawableRes icon: Int, text: CharSequence) {
        if (text.isNotBlank()) add(Stat(icon, text))
    }

    // Format a string resource and make every value substituted into it stand out, so the eye
    // lands on the name or the number instead of re-reading the sentence around it. The whole
    // substituted value is highlighted, not just its digits: languages disagree on whether the
    // unit comes before or after, and on whether it is a separate word at all
    private fun highlight(@StringRes resource: Int, vararg values: String): SpannableStringBuilder {
        return highlightIn(applicationContext.getString(resource, *values), *values)
    }

    private fun highlightIn(text: String, vararg values: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        var searchFrom = 0
        for (value in values) {
            if (value.isBlank()) continue
            val start = text.indexOf(value, searchFrom)
            if (start < 0) continue
            val end = start + value.length
            builder.setSpan(
                StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                ForegroundColorSpan(highlightColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            searchFrom = end
        }
        return builder
    }

    // Append ", " and a highlighted tail to a sentence that was already built
    private fun SpannableStringBuilder.appendHighlighted(value: String): SpannableStringBuilder {
        append(", ")
        val start = length
        append(value)
        setSpan(StyleSpan(Typeface.BOLD), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(
            ForegroundColorSpan(highlightColor), start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return this
    }

    private fun years(amount: Int): String =
        applicationContext.resources.getQuantityString(R.plurals.years, amount, amount)

    @DrawableRes
    private fun zodiacDrawable(signNumber: Int): Int = when (signNumber) {
        0 -> R.drawable.ic_zodiac_sagittarius
        1 -> R.drawable.ic_zodiac_capricorn
        2 -> R.drawable.ic_zodiac_aquarius
        3 -> R.drawable.ic_zodiac_pisces
        4 -> R.drawable.ic_zodiac_aries
        5 -> R.drawable.ic_zodiac_taurus
        6 -> R.drawable.ic_zodiac_gemini
        7 -> R.drawable.ic_zodiac_cancer
        8 -> R.drawable.ic_zodiac_leo
        9 -> R.drawable.ic_zodiac_virgo
        10 -> R.drawable.ic_zodiac_libra
        else -> R.drawable.ic_zodiac_scorpio
    }

    // The number of events for each type, or nothing if there are only birthdays
    private fun eventTypesNumbers(): CharSequence {
        if (anniversaries.isEmpty() &&
            deathAnniversaries.isEmpty() &&
            nameDays.isEmpty() &&
            others.isEmpty()
        ) return ""
        val summary = SpannableStringBuilder()

        fun appendType(label: Int, amount: Int, first: Boolean = false) {
            if (!first) summary.append(", ")
            summary.append(applicationContext.getString(label)).append(": ")
            val start = summary.length
            summary.append(amount.toString())
            summary.setSpan(
                StyleSpan(Typeface.BOLD), start, summary.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            summary.setSpan(
                ForegroundColorSpan(highlightColor), start, summary.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        appendType(R.string.birthday, birthdays.size, first = true)
        if (anniversaries.isNotEmpty()) appendType(R.string.anniversary, anniversaries.size)
        if (deathAnniversaries.isNotEmpty())
            appendType(R.string.death_anniversary, deathAnniversaries.size)
        if (nameDays.isNotEmpty()) appendType(R.string.name_day, nameDays.size)
        if (others.isNotEmpty()) appendType(R.string.other, others.size)
        return summary
    }

    // The average age
    private fun ageAverage(): CharSequence {
        val average = truncate(getAges().values.average()).toInt()
        return highlight(R.string.age_average, years(average))
    }

    // The oldest person, taking into account months and days
    private fun oldestPerson(): CharSequence {
        var oldestDate = LocalDate.now()
        var oldestName = ""
        var oldestAge = 0
        birthdays.forEach {
            if (oldestDate.isAfter(it.originalDate) &&
                it.yearMatter!! &&
                it.originalDate.isBefore(LocalDate.now())
            ) {
                oldestName = it.name
                oldestDate = it.originalDate
                oldestAge = getYears(it)
            }
        }
        if (oldestName.isBlank()) return ""
        return highlight(R.string.oldest_person, oldestName).appendHighlighted(years(oldestAge))
    }

    // The youngest person, taking into account months and days
    private fun youngestPerson(): CharSequence {
        var youngestDate = LocalDate.of(START_YEAR, 1, 1)
        var youngestName = ""
        var youngestAge = 0
        birthdays.forEach {
            if (youngestDate.isBefore(it.originalDate) &&
                it.yearMatter!! &&
                it.originalDate.isBefore(LocalDate.now())
            ) {
                youngestName = it.name
                youngestDate = it.originalDate
                youngestAge = getYears(it)
            }
        }
        if (youngestName.isBlank()) return ""
        val commonPart = highlight(R.string.youngest_person, youngestName)
        // If the youngest person is a baby, return the age in months
        return if (youngestAge == 0) {
            val months = getYearsMonths(youngestDate)
            commonPart.appendHighlighted(
                applicationContext.resources.getQuantityString(R.plurals.months, months, months)
            )
        } else commonPart.appendHighlighted(years(youngestAge))
    }

    // The most common month. When there's no common month, return a blank string
    private fun mostCommonMonth(): CharSequence {
        val months = mutableMapOf<String, Int>()
        birthdays.forEach {
            val month = it.originalDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            if (months[month] == null) months[month] = 1
            else months[month] = months[month]!!.plus(1)
        }
        val commonMonth: String = evaluateResult(months)
        if (commonMonth.isBlank()) return commonMonth
        return highlight(R.string.most_common_month, commonMonth)
    }

    // The most common age range (decade). When there's no common range, return a blank string
    private fun mostCommonAgeRange(): CharSequence {
        val ageRanges = mutableMapOf<String, Int>()
        birthdays.forEach {
            // Quite unnecessary both here and in other functions, but it's for extra safety
            if (it.yearMatter!!) {
                if (ageRanges[getAgeRange(it.originalDate)] == null) ageRanges[getAgeRange(it.originalDate)] =
                    1
                else ageRanges[getAgeRange(it.originalDate)] =
                    ageRanges[getAgeRange(it.originalDate)]!!.plus(1)
            }
        }
        val commonRange: String = evaluateResult(ageRanges)
        if (commonRange.isBlank()) return commonRange
        return highlight(
            R.string.most_common_age_range, commonRange, (commonRange.toInt() + 10).toString()
        )
    }

    // The most common decade (80s, 90s...). When there's no common decade, return a blank string
    private fun mostCommonDecade(): CharSequence {
        val decades = mutableMapOf<String, Int>()
        birthdays.forEach {
            // Quite unnecessary both here and in other functions, but it's for extra safety
            if (it.yearMatter!!) {
                if (decades[getDecade(it.originalDate)] == null) decades[getDecade(it.originalDate)] =
                    1
                else decades[getDecade(it.originalDate)] =
                    decades[getDecade(it.originalDate)]!!.plus(1)
            }
        }
        val commonDecade: String = evaluateResult(decades)
        if (commonDecade.isBlank()) return commonDecade
        return highlight(R.string.most_common_decade, commonDecade)
    }

    // Get a random "special age" person. Special age means 1, 10, 18, 20, 30, 40, and so on
    private fun specialAges(): CharSequence {
        val specialAges = arrayOf(1, 10, 18, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130)
        val specialPersons = mutableMapOf<String, Int>()
        birthdays.forEach {
            // Quite unnecessary both here and in other functions, but it's for extra safety
            if (it.yearMatter!!) {
                val nextAge = getNextYears(it)
                if (nextAge in specialAges) specialPersons[it.name] = nextAge
            }
        }
        return if (specialPersons.isEmpty()) ""
        else {
            val chosen = specialPersons.keys.random()
            val years = specialPersons[chosen]!!
            // Format the first half of the sentence
            highlight(R.string.special_ages, chosen).appendHighlighted(years(years))
        }
    }

    // Get the zodiac sign for a random person
    private fun zodiacSign(person: EventResult): CharSequence =
        highlight(R.string.random_zodiac_sign, person.name, getZodiacSign(person))

    // The most common zodiac sign. When there's no common zodiac sign, return a blank string
    private fun mostCommonZodiacSign(
        signNumber: Int = mostCommonZodiacSignNumber()
    ): CharSequence {
        if (signNumber < 0) return ""
        return highlight(R.string.most_common_zodiac_sign, zodiacSignName(signNumber))
    }

    // The winning sign as a number, or -1 when nothing wins outright. Counting by number instead of
    // by name is what lets the row draw the sign itself as its icon
    private fun mostCommonZodiacSignNumber(): Int {
        val counts = mutableMapOf<Int, Int>()
        birthdays.forEach {
            val sign = getZodiacSignNumber(it)
            counts[sign] = (counts[sign] ?: 0) + 1
        }
        val maxValue = counts.values.maxOrNull() ?: return -1
        if (counts.values.count { it == maxValue } > 1) return -1
        return counts.entries.first { it.value == maxValue }.key
    }

    // The most common chinese sign. When there's no common chinese sign, return a blank string
    private fun mostCommonChineseSign(): CharSequence {
        val chineseSigns = mutableMapOf<String, Int>()
        birthdays.forEach {
            if (chineseSigns[getChineseSign(it)] == null) chineseSigns[getChineseSign(it)] = 1
            else chineseSigns[getChineseSign(it)] = chineseSigns[getChineseSign(it)]!!.plus(1)
        }
        val commonChineseSign: String = evaluateResult(chineseSigns)
        if (commonChineseSign.isBlank()) return commonChineseSign
        return highlight(R.string.most_common_chinese_sign, commonChineseSign)
    }

    // Get the day of the week of birth for a random person
    private fun dayOfWeek(person: EventResult): CharSequence {
        return if (!person.yearMatter!!) ""
        else highlight(
            R.string.random_day_of_week, person.name,
            person.originalDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        )
    }

    // The most common day of the week of birth. When there's no common day of the week, return a blank string
    private fun mostCommonDayOfWeek(): CharSequence {
        val weekDays = mutableMapOf<String, Int>()
        birthdays.forEach {
            if (it.yearMatter!!) {
                val weekDay =
                    it.originalDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                if (weekDays[weekDay] == null) weekDays[weekDay] = 1
                else weekDays[weekDay] = weekDays[weekDay]!!.plus(1)
            }
        }
        val commonWeekDay: String = evaluateResult(weekDays)
        if (commonWeekDay.isBlank()) return commonWeekDay
        return highlight(R.string.most_common_day_of_week, commonWeekDay)
    }

    // Get the number of persons born in a leap year. Even 0 is an acceptable result
    private fun leapYearTotal(): CharSequence {
        var leapTotal = 0
        birthdays.forEach {
            if (it.yearMatter!!) if (it.originalDate.isLeapYear) leapTotal++
        }
        return highlightIn(
            applicationContext.resources.getQuantityString(
                R.plurals.leap_year_total, leapTotal, leapTotal
            ),
            leapTotal.toString()
        )
    }

    // Get the chinese year of a random person
    private fun chineseSign(person: EventResult): CharSequence {
        return if (!person.yearMatter!!) ""
        else highlight(R.string.random_chinese_year, person.name, getChineseSign(person))
    }

    // Get a list containing the names and an int containing the age
    private fun getAges(): Map<String, Int> {
        val ages = mutableMapOf<String, Int>()
        birthdays.forEach {
            if (it.yearMatter!!) {
                val age = getYears(it)
                ages[it.name] = age
            }
        }
        return ages
    }

    fun getChineseSign(person: EventResult): String {
        return when (chineseAnimal(person.originalDate)) {
            0 -> applicationContext.getString(R.string.chinese_zodiac_rat)
            1 -> applicationContext.getString(R.string.chinese_zodiac_ox)
            2 -> applicationContext.getString(R.string.chinese_zodiac_tiger)
            3 -> applicationContext.getString(R.string.chinese_zodiac_rabbit)
            4 -> applicationContext.getString(R.string.chinese_zodiac_dragon)
            5 -> applicationContext.getString(R.string.chinese_zodiac_snake)
            6 -> applicationContext.getString(R.string.chinese_zodiac_horse)
            7 -> applicationContext.getString(R.string.chinese_zodiac_goat)
            8 -> applicationContext.getString(R.string.chinese_zodiac_monkey)
            9 -> applicationContext.getString(R.string.chinese_zodiac_rooster)
            10 -> applicationContext.getString(R.string.chinese_zodiac_dog)
            11 -> applicationContext.getString(R.string.chinese_zodiac_pig)
            else -> throw Exception("Unexpected Chinese animal index")
        }
    }

    // Get the zodiac sign
    fun getZodiacSign(person: EventResult): String =
        zodiacSignName(getZodiacSignNumber(person))

    private fun zodiacSignName(signNumber: Int): String = applicationContext.getString(
        when (signNumber) {
            0 -> R.string.zodiac_sagittarius
            1 -> R.string.zodiac_capricorn
            2 -> R.string.zodiac_aquarius
            3 -> R.string.zodiac_pisces
            4 -> R.string.zodiac_aries
            5 -> R.string.zodiac_taurus
            6 -> R.string.zodiac_gemini
            7 -> R.string.zodiac_cancer
            8 -> R.string.zodiac_leo
            9 -> R.string.zodiac_virgo
            10 -> R.string.zodiac_libra
            else -> R.string.zodiac_scorpio
        }
    )

    // Only return the number of the sign
    fun getZodiacSignNumber(person: EventResult): Int {
        val day = person.originalDate.dayOfMonth
        val month = person.originalDate.month.value
        var signNumber = 0
        when (month) {
            12 -> signNumber = if (day <= 21) 0 else 1
            1 -> signNumber = if (day <= 20) 1 else 2
            2 -> signNumber = if (day <= 18) 2 else 3
            3 -> signNumber = if (day <= 20) 3 else 4
            4 -> signNumber = if (day <= 20) 4 else 5
            5 -> signNumber = if (day <= 20) 5 else 6
            6 -> signNumber = if (day <= 21) 6 else 7
            7 -> signNumber = if (day <= 22) 7 else 8
            8 -> signNumber = if (day <= 23) 8 else 9
            9 -> signNumber = if (day <= 22) 9 else 10
            10 -> signNumber = if (day <= 22) 10 else 11
            11 -> signNumber = if (day <= 22) 11 else 0
        }
        return signNumber
    }

    // Evaluate the result, differently from maxBy. If there's a tie, return an empty string
    private fun evaluateResult(map: Map<String, Int>): String {
        var maxValue = 0
        var result = ""
        map.forEach {
            if (it.value > maxValue) {
                maxValue = it.value
                result = it.key
            }
        }
        return if (map.values.count { it == maxValue } > 1) ""
        else result
    }

    // Return the list filtering the birthdays
    private fun filterBirthdays(): List<EventResult> {
        return events.filter { isBirthday(it) }
    }

    // Return the list filtering the anniversary
    private fun filterAnniversaries(): List<EventResult> {
        return events.filter { isAnniversary(it) }
    }

    // Return the list filtering the death anniversaries
    private fun filterDeathAnniversaries(): List<EventResult> {
        return events.filter { isDeathAnniversary(it) }
    }

    // Return the list filtering the name days
    private fun filterNameDays(): List<EventResult> {
        return events.filter { isNameDay(it) }
    }

    // Return the list filtering the "others"
    private fun filterOthers(): List<EventResult> {
        return events.filter { isOther(it) }
    }

}
