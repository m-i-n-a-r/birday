package com.minar.birday.model

data class EventType(val codeName: EventCode, val value: String) {
    override fun toString(): String = value // The value shown in the spinner
}
