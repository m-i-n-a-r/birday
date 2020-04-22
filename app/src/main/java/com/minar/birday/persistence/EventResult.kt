package com.minar.birday.persistence

import java.time.LocalDate

data class EventResult (
    val id: Int,
    val type: String? = "birthday",
    val name: String,
    val surname: String? = "",
    val favorite: Boolean? = false,
    val yearMatter: Boolean? = true,
    val originalDate: LocalDate,
    val nextDate: LocalDate? = null
)