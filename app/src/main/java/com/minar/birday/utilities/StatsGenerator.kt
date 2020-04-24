package com.minar.birday.utilities

import com.minar.birday.persistence.EventResult
import kotlin.random.Random

class StatsGenerator(eventList: List<EventResult>) {

    // TODO generate the stats!
    fun generateRandomStat(): String {
        return when(Random.nextInt(0, 6)) {
            1 -> ageAverage()
            2 -> mostCommonMonth()
            3 -> mostCommonDecade()
            4 -> mostCommonAgeRange()
            5 -> specialAges()
            else -> ageAverage()
        }
    }

    private fun ageAverage(): String {
        return "placeholder"
    }

    private fun mostCommonMonth(): String {
        return "placeholder"
    }

    private fun mostCommonAgeRange(): String {
        return "placeholder"
    }

    private fun mostCommonDecade(): String {
        return "placeholder"
    }

    private fun specialAges(): String {
        return "placeholder"
    }
}