package com.minar.birday.model

// A wrapper around EventResult, to also consider the month header as an object
sealed class EventDataItem {
    data class EventItem(val eventResult: EventResult) : EventDataItem() {
        override val id = eventResult.id.toLong()
    }

    data class IndexHeader(val headerTitle: String) : EventDataItem() {
        override val id = Long.MIN_VALUE
    }

    abstract val id: Long
}