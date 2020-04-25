package com.minar.birday.utilities

import com.minar.birday.persistence.EventResult
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.truncate
import kotlin.random.Random

class StatsGenerator(eventList: List<EventResult>) {
    private val events: List<EventResult> = eventList

    // TODO test every function
    // Generate a random stat choosing randomly between one of the available functions
    fun generateRandomStat(): String {
        // Use a response string to re-execute the stats calculation if a stat cannot be computed correctly
        var response: String? = null
        while (response.isNullOrBlank()) {
            response = when (Random.nextInt(0, 11)) {
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
                else -> ageAverage()
            }
        }
        return response
    }

    // The average age
    private fun ageAverage(): String {
        val average = truncate(getAges().values.average())
        return average.toString()
    }

    // The most common month. When there's no common month, return a blank string
    private fun mostCommonMonth(): String {
        val months = mutableMapOf<String, Int>()
        val commonMonth: String
        events.forEach {
            if(months[it.originalDate.month.toString()] == null) months[it.originalDate.month.toString()] = 1
            else months[it.originalDate.month.toString()] = months[it.originalDate.month.toString()]!!.plus(1)
        }
        commonMonth = months.maxBy { it.value }!!.key
        return commonMonth
    }

    // The most common age range (decade). When there's no common range, return a blank string
    private fun mostCommonAgeRange(): String {
        val ageRanges = mutableMapOf<String, Int>()
        val commonRange: String
        events.forEach {
            if(ageRanges[getAgeRange(it.originalDate)] == null) ageRanges[getAgeRange(it.originalDate)] = 1
            else ageRanges[getAgeRange(it.originalDate)] = ageRanges[getAgeRange(it.originalDate)]!!.plus(1)
        }
        commonRange = ageRanges.maxBy { it.value }!!.key
        return commonRange
    }

    // The most common decade (80s, 90s..). When there's no common decade, return a blank string
    private fun mostCommonDecade(): String {
        val decades = mutableMapOf<String, Int>()
        val commonDecade: String
        events.forEach {
            if(decades[getDecade(it.originalDate)] == null) decades[getDecade(it.originalDate)] = 1
            else decades[getDecade(it.originalDate)] = decades[getDecade(it.originalDate)]!!.plus(1)
        }
        commonDecade = decades.maxBy { it.value }!!.key
        return commonDecade
    }

    // TODO finish this
    // Get a random "special age" person. Special age means 10, 18, 20, 30, 40, and so on
    private fun specialAges(): String {
        val specialAges = arrayOf(10,18,20,30,40,50,60,70,80,90,100,110,120,130)
        val specialAgePerson = ""
        events.forEach {
            it.nextDate
        }
        return specialAgePerson
    }

    // Get the zodiac sign for a random person
    private fun randomZodiacSign(): String {
        val randomPerson = events.random()
        return getZodiacSign(randomPerson.originalDate.dayOfMonth, randomPerson.originalDate.month.value) + ": " + randomPerson.name
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
        commonZodiacSign = zodiacSigns.maxBy { it.value }!!.key
        return commonZodiacSign
    }

    // Get the day of the week of birth for a random person
    private fun randomDayOfWeek(): String {
        return events.random().originalDate.dayOfWeek.toString()
    }

    // The most common day of the week of birth. When there's no common day of the week, return a blank string
    private fun mostCommonDayOfWeek(): String {
        val weekDays = mutableMapOf<String, Int>()
        val commonWeekDay: String
        events.forEach {
            if(weekDays[it.originalDate.dayOfWeek.toString()] == null) weekDays[it.originalDate.dayOfWeek.toString()] = 1
            else weekDays[it.originalDate.dayOfWeek.toString()] = weekDays[it.originalDate.dayOfWeek.toString()]!!.plus(1)
        }
        commonWeekDay = weekDays.maxBy { it.value }!!.key
        return commonWeekDay
    }

    // Get the number of persons born in a leap year. Even 0 is an acceptable result
    private fun leapYearTotal(): String {
        var leapTotal = 0
        events.forEach {
            if (it.originalDate.isLeapYear) leapTotal++
        }
        return leapTotal.toString()
    }

    // Get a list containing all the ages without any reference to the names
    private fun getAges(): Map<String, Int> {
        val ages = mutableMapOf<String, Int>()
        events.forEach {
            val age = it.nextDate!!.year - it.originalDate.year
            ages[it.name] = age
        }
        return ages
    }

    private fun getDecade(originalDate: LocalDate) = ((originalDate.year.toDouble() / 10).roundToInt() * 10).toString()

    private fun getAgeRange(originalDate: LocalDate) = (((LocalDate.now().year - originalDate.year).toDouble() / 10).roundToInt() * 10).toString()

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
            11 ->  sign = if (day < 22) "Scorpio" else "Sagittarius"
        }
        return sign
    }
}