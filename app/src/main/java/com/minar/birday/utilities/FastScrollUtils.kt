package com.minar.birday.utilities

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale


// The short name of the month, capitalized as a title, for the bubble of the fast scroll
fun fastScrollMonth(date: LocalDate): String =
    date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
