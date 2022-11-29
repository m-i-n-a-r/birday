package com.minar.birday.utilities

import android.content.Context
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.annotation.ColorInt
import com.minar.birday.R
import com.minar.birday.model.EventResult
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.math.truncate
import kotlin.random.Random

// Generate a series of stats based on a list of events and focused on birthdays
class StatsGenerator(
    eventList: List<EventResult>,
    context: Context?,
    private val astrologyDisabled: Boolean = false
) {
    private val events: List<EventResult> = eventList
    private val birthdays = filterBirthdays()
    private val anniversaries = filterAnniversaries()
    private val deathAnniversaries = filterDeathAnniversaries()
    private val nameDays = filterNameDays()
    private val others = filterOthers()
    private val applicationContext = context

    // Generate a random stat choosing randomly between one of the available functions
    fun generateRandomStat(): String {
        // Use a response string to re-execute the stats calculation if a stat cannot be computed correctly
        var response: String? = null
        val randomPerson = birthdays.random()
        while (response.isNullOrBlank()) {
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
        return response
    }

    // Generate a summary of the cumulative stats
    fun generateFullStats(): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        val stats = mutableListOf<String>()
        stats.add(ageAverage())
        stats.add(oldestPerson())
        stats.add(youngestPerson())
        stats.add(mostCommonAgeRange())
        stats.add(mostCommonDayOfWeek())
        stats.add(mostCommonDecade())
        stats.add(mostCommonMonth())
        stats.add(leapYearTotal())
        // Only include astrology related stats if astrology is enabled
        if (!astrologyDisabled) {
            stats.add(mostCommonZodiacSign())
            stats.add(mostCommonChineseSign())
        }
        stats.add(eventTypesNumbers())
        stats.removeIf { it.isBlank() }
        sb.appendBulletSpans(
            stats,
            16,
            getThemeColor(R.attr.colorOnSurfaceVariant, applicationContext!!)
        )
        return sb
    }

    // The number of events for each type, or nothing if there are only birthdays
    private fun eventTypesNumbers(): String {
        if (anniversaries.isEmpty() &&
            deathAnniversaries.isEmpty() &&
            nameDays.isEmpty() &&
            others.isEmpty()
        ) return ""
        val typesSummary = SpannableStringBuilder()
        typesSummary.append("${applicationContext?.getString(R.string.birthday)}: ${birthdays.size}")
        if (anniversaries.isNotEmpty())
            typesSummary.append(", ${applicationContext?.getString(R.string.anniversary)}: ${anniversaries.size}")
        if (deathAnniversaries.isNotEmpty())
            typesSummary.append(", ${applicationContext?.getString(R.string.death_anniversary)}: ${deathAnniversaries.size}")
        if (nameDays.isNotEmpty())
            typesSummary.append(", ${applicationContext?.getString(R.string.name_day)}: ${nameDays.size}")
        if (others.isNotEmpty())
            typesSummary.append(", ${applicationContext?.getString(R.string.other)}: ${others.size}")
        return typesSummary.toString()
    }

    // The average age
    private fun ageAverage(): String {
        val average = truncate(getAges().values.average()).toInt()
        return String.format(
            applicationContext?.getString(R.string.age_average)!!,
            applicationContext.resources?.getQuantityString(R.plurals.years, average, average),
        )
    }

    // The oldest person, taking into account months and days
    private fun oldestPerson(): String {
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
        return String.format(
            applicationContext?.getString(R.string.oldest_person)!!,
            oldestName,
        ) + ", " + applicationContext.resources?.getQuantityString(
            R.plurals.years,
            oldestAge,
            oldestAge
        ).toString()
    }

    // The youngest person, taking into account months and days
    private fun youngestPerson(): String {
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
        val commonPart = String.format(
            applicationContext?.getString(R.string.youngest_person)!!,
            youngestName,
        )
        // If the youngest person is a baby, return the age in months
        return if (youngestAge == 0) {
            val months = getYearsMonths(youngestDate)
            "$commonPart, " + applicationContext.resources?.getQuantityString(
                R.plurals.months,
                months,
                months
            )
        } else {
            "$commonPart, " + applicationContext.resources?.getQuantityString(
                R.plurals.years,
                youngestAge,
                youngestAge
            )
        }
    }

    // The most common month. When there's no common month, return a blank string
    private fun mostCommonMonth(): String {
        val months = mutableMapOf<String, Int>()
        birthdays.forEach {
            val month = it.originalDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            if (months[month] == null) months[month] = 1
            else months[month] = months[month]!!.plus(1)
        }
        val commonMonth: String = evaluateResult(months)
        if (commonMonth.isBlank()) return commonMonth
        return String.format(
            applicationContext?.getString(R.string.most_common_month)!!,
            commonMonth,
        )
    }

    // The most common age range (decade). When there's no common range, return a blank string
    private fun mostCommonAgeRange(): String {
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
        return String.format(
            applicationContext?.getString(R.string.most_common_age_range)!!,
            commonRange,
            commonRange.toInt() + 10,
        )
    }

    // The most common decade (80s, 90s..). When there's no common decade, return a blank string
    private fun mostCommonDecade(): String {
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
        return String.format(
            applicationContext?.getString(R.string.most_common_decade)!!,
            commonDecade,
        )
    }

    // Get a random "special age" person. Special age means 1, 10, 18, 20, 30, 40, and so on
    private fun specialAges(): String {
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
            return String.format(
                applicationContext?.getString(R.string.special_ages)!!,
                chosen,
            ) + ", " + applicationContext.resources?.getQuantityString(
                R.plurals.years,
                years,
                years
            ).toString()
        }
    }

    // Get the zodiac sign for a random person
    private fun zodiacSign(person: EventResult): String {
        return String.format(
            applicationContext?.getString(R.string.random_zodiac_sign)!!,
            person.name,
            getZodiacSign(person),
        )
    }

    // The most common zodiac sign. When there's no common zodiac sign, return a blank string
    private fun mostCommonZodiacSign(): String {
        val zodiacSigns = mutableMapOf<String, Int>()
        birthdays.forEach {
            if (zodiacSigns[getZodiacSign(it)] == null) zodiacSigns[getZodiacSign(it)] = 1
            else zodiacSigns[getZodiacSign(it)] = zodiacSigns[getZodiacSign(it)]!!.plus(1)
        }
        val commonZodiacSign: String = evaluateResult(zodiacSigns)
        if (commonZodiacSign.isBlank()) return commonZodiacSign
        return String.format(
            applicationContext?.getString(R.string.most_common_zodiac_sign)!!,
            commonZodiacSign,
        )
    }

    // The most common chinese sign. When there's no common chinese sign, return a blank string
    private fun mostCommonChineseSign(): String {
        val chineseSigns = mutableMapOf<String, Int>()
        birthdays.forEach {
            if (chineseSigns[getChineseSign(it)] == null) chineseSigns[getChineseSign(it)] = 1
            else chineseSigns[getChineseSign(it)] = chineseSigns[getChineseSign(it)]!!.plus(1)
        }
        val commonChineseSign: String = evaluateResult(chineseSigns)
        if (commonChineseSign.isBlank()) return commonChineseSign
        return String.format(
            applicationContext?.getString(R.string.most_common_chinese_sign)!!,
            commonChineseSign,
        )
    }

    // Get the day of the week of birth for a random person
    private fun dayOfWeek(person: EventResult): String {
        return if (!person.yearMatter!!) ""
        else String.format(
            applicationContext?.getString(R.string.random_day_of_week)!!,
            person.name,
            person.originalDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
        )
    }

    // The most common day of the week of birth. When there's no common day of the week, return a blank string
    private fun mostCommonDayOfWeek(): String {
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
        return String.format(
            applicationContext?.getString(R.string.most_common_day_of_week)!!,
            commonWeekDay
        )
    }

    // Get the number of persons born in a leap year. Even 0 is an acceptable result
    private fun leapYearTotal(): String {
        var leapTotal = 0
        birthdays.forEach {
            if (it.yearMatter!!) if (it.originalDate.isLeapYear) leapTotal++
        }
        return applicationContext?.resources?.getQuantityString(
            R.plurals.leap_year_total,
            leapTotal,
            leapTotal
        ).toString()
    }

    // Get the chinese year of a random person
    private fun chineseSign(person: EventResult): String {
        return if (!person.yearMatter!!) ""
        else String.format(
            applicationContext?.getString(R.string.random_chinese_year)!!,
            person.name,
            getChineseSign(person),
        )
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
            0 -> applicationContext!!.getString(R.string.chinese_zodiac_rat)
            1 -> applicationContext!!.getString(R.string.chinese_zodiac_ox)
            2 -> applicationContext!!.getString(R.string.chinese_zodiac_tiger)
            3 -> applicationContext!!.getString(R.string.chinese_zodiac_rabbit)
            4 -> applicationContext!!.getString(R.string.chinese_zodiac_dragon)
            5 -> applicationContext!!.getString(R.string.chinese_zodiac_snake)
            6 -> applicationContext!!.getString(R.string.chinese_zodiac_horse)
            7 -> applicationContext!!.getString(R.string.chinese_zodiac_goat)
            8 -> applicationContext!!.getString(R.string.chinese_zodiac_monkey)
            9 -> applicationContext!!.getString(R.string.chinese_zodiac_rooster)
            10 -> applicationContext!!.getString(R.string.chinese_zodiac_dog)
            11 -> applicationContext!!.getString(R.string.chinese_zodiac_pig)
            else -> throw Exception("Unexpected Chinese animal index")
        }
    }

    // Get the zodiac sign
    fun getZodiacSign(person: EventResult): String {
        var sign = ""
        when (getZodiacSignNumber(person)) {
            0 -> sign = applicationContext?.getString(R.string.zodiac_sagittarius).toString()
            1 -> sign = applicationContext?.getString(R.string.zodiac_capricorn).toString()
            2 -> sign = applicationContext?.getString(R.string.zodiac_aquarius).toString()
            3 -> sign = applicationContext?.getString(R.string.zodiac_pisces).toString()
            4 -> sign = applicationContext?.getString(R.string.zodiac_aries).toString()
            5 -> sign = applicationContext?.getString(R.string.zodiac_taurus).toString()
            6 -> sign = applicationContext?.getString(R.string.zodiac_gemini).toString()
            7 -> sign = applicationContext?.getString(R.string.zodiac_cancer).toString()
            8 -> sign = applicationContext?.getString(R.string.zodiac_leo).toString()
            9 -> sign = applicationContext?.getString(R.string.zodiac_virgo).toString()
            10 -> sign = applicationContext?.getString(R.string.zodiac_libra).toString()
            11 -> sign = applicationContext?.getString(R.string.zodiac_scorpio).toString()
        }
        return sign
    }

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

    // Functions to build the statistics in a bullet list
    private fun SpannableStringBuilder.appendBulletSpans(
        paragraphs: List<String>,
        margin: Int,
        @ColorInt color: Int
    ): SpannableStringBuilder {
        for (paragraph in paragraphs) {
            if (paragraphs.indexOf(paragraph) == 0) appendBulletSpan(paragraph, margin, color, true)
            else appendBulletSpan(paragraph, margin, color)
        }
        return this
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

    // Prepare the bulleted list
    private fun SpannableStringBuilder.appendBulletSpan(
        paragraph: String,
        margin: Int,
        @ColorInt color: Int,
        first: Boolean = false
    ): SpannableStringBuilder {
        if (!first) append("\n")
        val bulletSpan =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) BulletSpan(margin, color, 12)
            else BulletSpan(margin, color)
        val spaceBefore = length
        append(paragraph)
        val spaceAfter = length
        append("\n")
        setSpan(bulletSpan, spaceBefore, spaceAfter, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return this
    }
}
