package com.minar.birday.model

import java.time.LocalDate

// A wrapper around EventResult, to also consider the month header as an object
sealed class EventDataItem {
    data class EventItem(val eventResult: EventResult) : EventDataItem() {
        override val id = eventResult.id.toLong()
    }

    data class MonthHeader(val startDate: LocalDate) : EventDataItem() {
        override val id = Long.MIN_VALUE
    }

    abstract val id: Long
}