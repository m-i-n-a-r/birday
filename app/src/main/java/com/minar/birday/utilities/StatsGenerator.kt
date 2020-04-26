package com.minar.birday.utilities

import android.content.Context
import com.minar.birday.R
import com.minar.birday.persistence.EventResult
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.math.truncate
import kotlin.random.Random

class StatsGenerator(eventList: List<EventResult>, context: Context?) {
    private val events: List<EventResult> = eventList
    private val applicationContext = context

    // Generate a random stat choosing randomly between one of the available functions
    fun generateRandomStat(): String {
        // Use a response string to re-execute the stats calculation if a stat cannot be computed correctly
        var response: String? = null
        while (response.isNullOrBlank()) {
            response = when (Random.nextInt(0, 12)) {
                1 -> ageAverage()
                2 -> mostCommonMonth()
                3 -> mostCommonDecade()
                4 -> mostCommonAgeRange()
                5 -> specialAges()
                6 -> randomZodiacSign()
                7 -> mostCommonZodiacSign()
                8 -> randomDayOfWeek()
                9 -> mostCommonDayOfWeek()
                10 -> leapYearTotal()
                11 -> randomChineseYear()
                else -> ageAverage()
            }
        }
        return response
    }

    // The average age
    private fun ageAverage(): String {
        val average = truncate(getAges().values.average())
        return applicationContext?.getString(R.string.age_average) + " " + average.toString() + " " + applicationContext?.getString(R.string.years)
    }

    // The most common month. When there's no common month, return a blank string
    private fun mostCommonMonth(): String {
        val months = mutableMapOf<String, Int>()
        val commonMonth: String
        events.forEach {
            val month = it.originalDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            if(months[month] == null) months[month] = 1
            else months[month] = months[month]!!.plus(1)
        }
        commonMonth = evaluateResult(months)
        if (commonMonth.isBlank()) return commonMonth
        return applicationContext?.getString(R.string.most_common_month) + " " + commonMonth.toLowerCase(Locale.ROOT)
    }

    // The most common age range (decade). When there's no common range, return a blank string
    private fun mostCommonAgeRange(): String {
        val ageRanges = mutableMapOf<String, Int>()
        val commonRange: String
        events.forEach {
            if(ageRanges[getAgeRange(it.originalDate)] == null) ageRanges[getAgeRange(it.originalDate)] = 1
            else ageRanges[getAgeRange(it.originalDate)] = ageRanges[getAgeRange(it.originalDate)]!!.plus(1)
        }
        commonRange = evaluateResult(ageRanges)
        if (commonRange.isBlank()) return commonRange
        return applicationContext?.getString(R.string.most_common_age_range) + " " + commonRange + "-" + (commonRange.toInt() + 10)
    }

    // The most common decade (80s, 90s..). When there's no common decade, return a blank string
    private fun mostCommonDecade(): String {
        val decades = mutableMapOf<String, Int>()
        val commonDecade: String
        events.forEach {
            if(decades[getDecade(it.originalDate)] == null) decades[getDecade(it.originalDate)] = 1
            else decades[getDecade(it.originalDate)] = decades[getDecade(it.originalDate)]!!.plus(1)
        }
        commonDecade = evaluateResult(decades)
        if (commonDecade.isBlank()) return commonDecade
        return applicationContext?.getString(R.string.most_common_decade) + " " + commonDecade
    }

    // Get a random "special age" person. Special age means 10, 18, 20, 30, 40, and so on
    private fun specialAges(): String {
        val specialAges = arrayOf(10,18,20,30,40,50,60,70,80,90,100,110,120,130)
        val specialPersons = mutableMapOf<String, Int>()
        events.forEach {
            val nextAge = getNextAge(it)
            if (nextAge in specialAges) specialPersons[it.name] = nextAge
        }
        return if (specialPersons.isEmpty()) ""
        else {
            val chosen = specialPersons.keys.random()
            applicationContext?.getString(R.string.special_ages) + " " + chosen + ", " +
                    specialPersons[chosen] + " " + applicationContext?.getString(R.string.years)
        }
    }

    // Get the zodiac sign for a random person
    private fun randomZodiacSign(): String {
        val randomPerson = events.random()
        return applicationContext?.getString(R.string.random_zodiac_sign) + " " + randomPerson.name + ": " +
                getZodiacSign(randomPerson.originalDate.dayOfMonth, randomPerson.originalDate.month.value)
    }

    // The most common zodiac sign. When there's no common zodiac sign, return a blank string
    private fun mostCommonZodiacSign(): String {
        val zodiacSigns = mutableMapOf<String, Int>()
        val commonZodiacSign: String
        events.forEach {
            if(zodiacSigns[getZodiacSign(it.originalDate.dayOfMonth, it.originalDate.month.value)] == null)
                zodiacSigns[getZodiacSign(it.originalDate.dayOfMonth, it.originalDate.month.value)] = 1
            else zodiacSigns[getZodiacSign(it.originalDate.dayOfMonth, it.originalDate.month.value)] =
                zodiacSigns[getZodiacSign(it.originalDate.dayOfMonth, it.originalDate.month.value)]!!.plus(1)
        }
        commonZodiacSign = evaluateResult(zodiacSigns)
        if (commonZodiacSign.isBlank()) return commonZodiacSign
        return applicationContext?.getString(R.string.most_common_zodiac_sign) + " " + commonZodiacSign
    }

    // Get the day of the week of birth for a random person
    private fun randomDayOfWeek(): String {
        val randomPerson = events.random()
        return randomPerson.name + " " + applicationContext?.getString(R.string.random_day_of_week) + " " +
                randomPerson.originalDate.dayOfWeek.toString().toLowerCase(Locale.ROOT)
    }

    // The most common day of the week of birth. When there's no common day of the week, return a blank string
    private fun mostCommonDayOfWeek(): String {
        val weekDays = mutableMapOf<String, Int>()
        val commonWeekDay: String
        events.forEach {
            val weekDay = it.originalDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            if(weekDays[weekDay] == null) weekDays[weekDay] = 1
            else weekDays[weekDay] = weekDays[weekDay]!!.plus(1)
        }
        commonWeekDay = evaluateResult(weekDays)
        if (commonWeekDay.isBlank()) return commonWeekDay
        return applicationContext?.getString(R.string.most_common_day_of_week) + " " + commonWeekDay.toLowerCase(
            Locale.ROOT)
    }

    // Get the number of persons born in a leap year. Even 0 is an acceptable result
    private fun leapYearTotal(): String {
        var leapTotal = 0
        events.forEach {
            if (it.originalDate.isLeapYear) leapTotal++
        }
        return applicationContext?.getString(R.string.leap_year_total) + " " + leapTotal.toString()
    }

    // Get the chinese year of a random person
    private fun randomChineseYear(): String {
        val randomPerson = events.random()
        var sign = ""
        var signNumber = 0
        when (randomPerson.originalDate.year % 12) {
            4 -> signNumber = 0
            5 -> signNumber = 1
            6 -> signNumber = 2
            7 -> signNumber = 3
            8 -> signNumber = 4
            9 -> signNumber = 5
            10 -> signNumber = 6
            11 -> signNumber = 7
            0 -> signNumber = 8
            1 -> signNumber = 9
            2 -> signNumber = 10
            3 -> signNumber = 11
        }
        when (signNumber) {
            0 -> sign = applicationContext?.getString(R.string.chinese_zodiac_rat).toString()
            1 -> sign = applicationContext?.getString(R.string.chinese_zodiac_ox).toString()
            2 -> sign = applicationContext?.getString(R.string.chinese_zodiac_tiger).toString()
            3 -> sign = applicationContext?.getString(R.string.chinese_zodiac_rabbit).toString()
            4 -> sign = applicationContext?.getString(R.string.chinese_zodiac_dragon).toString()
            5 -> sign = applicationContext?.getString(R.string.chinese_zodiac_snake).toString()
            6 -> sign = applicationContext?.getString(R.string.chinese_zodiac_horse).toString()
            7 -> sign = applicationContext?.getString(R.string.chinese_zodiac_goat).toString()
            8 -> sign = applicationContext?.getString(R.string.chinese_zodiac_monkey).toString()
            9 -> sign = applicationContext?.getString(R.string.chinese_zodiac_rooster).toString()
            10 -> sign = applicationContext?.getString(R.string.chinese_zodiac_dog).toString()
            11 -> sign = applicationContext?.getString(R.string.chinese_zodiac_pig).toString()
        }
        return applicationContext?.getString(R.string.random_chinese_year) + " " + randomPerson.name + ": " + sign
    }

    // Get a list containing all the ages without any reference to the names
    private fun getAges(): Map<String, Int> {
        val ages = mutableMapOf<String, Int>()
        events.forEach {
            val age = getAge(it)
            ages[it.name] = age
        }
        return ages
    }

    private fun getAge(eventResult: EventResult) = eventResult.nextDate!!.year - eventResult.originalDate.year - 1

    private fun getNextAge(eventResult: EventResult) = eventResult.nextDate!!.year - eventResult.originalDate.year

    private fun getDecade(originalDate: LocalDate) = ((originalDate.year.toDouble() / 10).toInt() * 10).toString()

    private fun getAgeRange(originalDate: LocalDate) = (((LocalDate.now().year - originalDate.year).toDouble() / 10).toInt() * 10).toString()

    private fun getZodiacSign(day: Int, month: Int): String {
        var sign = ""
        when (month) {
            12 -> sign = if (day < 22) "Sagittarius" else "Capricorn"
            1 -> sign = if (day < 20) "Capricorn" else "Aquarius"
            2 -> sign = if (day < 19) "Aquarius" else "Pisces"
            3 -> sign = if (day < 21) "Pisces" else "Aries"
            4 -> sign = if (day < 20) "Aries" else "Taurus"
            5 -> sign = if (day < 21) "Taurus" else "Gemini"
            6 -> sign = if (day < 21) "Gemini" else "Cancer"
            7 -> sign = if (day < 23) "Cancer" else "Leo"
            8 -> sign = if (day < 23) "Leo" else "Virgo"
            9 -> sign = if (day < 23) "Virgo" else "Libra"
            10 -> sign = if (day < 23) "Libra" else "Scorpio"
            11 -> sign = if (day < 22) "Scorpio" else "Sagittarius"
        }
        return sign
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
        return if(map.values.count { it == maxValue } > 1) ""
        else result
    }
}