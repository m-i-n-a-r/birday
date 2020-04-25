package com.minar.birday.utilities

import com.minar.birday.persistence.EventResult
import kotlin.random.Random

class StatsGenerator(eventList: List<EventResult>) {
    private val events: List<EventResult> = eventList

    // TODO generate the stats!
    // Generate a random stat choosing randomly between one of the available functions
    fun generateRandomStat(): String {
        // Use a response string to re-execute the stats calculation if a stat cannot be computed correctly
        var response = ""
        while (response.isBlank()) {
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
        val average = getAges().values.average()
        return average.toString()
    }

    // The most common month. When there's no common month, simply state it
    private fun mostCommonMonth(): String {
        events.forEach {
            it.originalDate.monthValue
        }
        return "placeholder"
    }

    private fun mostCommonAgeRange(): String {
        events.forEach {

        }
        return "placeholder"
    }

    private fun mostCommonDecade(): String {
        events.forEach {

        }
        return "placeholder"
    }

    // Get a random "special age" person. Special age means 10, 18, 20, 30, 40, and so on
    private fun specialAges(): String {
        val specialAges = arrayOf(10,18,20,30,40,50,60,70,80,90,100,110,120,130)
        events.forEach {
            it.nextDate
        }
        return "placeholder"
    }

    private fun randomZodiacSign(): String {
        return "placeholder"
    }

    private fun mostCommonZodiacSign(): String {
        return "placeholder"
    }

    private fun randomDayOfWeek(): String {
        return "placeholder"
    }

    private fun mostCommonDayOfWeek(): String {
        return "placeholder"
    }

    private fun leapYearTotal(): String {
        return "placeholder"
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

}