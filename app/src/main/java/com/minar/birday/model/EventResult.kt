package com.minar.birday.model

import java.time.LocalDate

data class EventResult (
    val id: Int,
    val type: String? = "birthday",
    var name: String,
    var surname: String? = "",
    var favorite: Boolean? = false,
    val yearMatter: Boolean? = true,
    var originalDate: LocalDate,
    val nextDate: LocalDate? = null
)