package com.minar.birday.utilities

import com.minar.birday.model.EventResult
import java.time.LocalDate
import java.time.Period

// Get the age also considering the possible corner cases
fun getAge(eventResult: EventResult): Int {
    var age = -1
    if (!eventResult.yearMatter!!) return age
    else age = eventResult.nextDate!!.year - eventResult.originalDate.year - 1
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