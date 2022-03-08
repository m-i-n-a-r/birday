package com.minar.birday.utilities

import java.util.*

// Extension function to quickly capitalize a name, also considering other uppercase letter, multiple words and the apostrophe
@ExperimentalStdlibApi
fun String.smartCapitalize(): String {
    return trim().split(" ")
        .joinToString(" ") { it ->
            it.lowercase(Locale.ROOT)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        .split("'").joinToString("'") { it ->
            it.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT
                ) else it.toString()
            }
        }
        .split("-").joinToString("-") { it ->
            it.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.ROOT
            ) else it.toString()
        } }
}

// Simply checks if the string is written using only letters, numbers, emoticons and at most one apostrophe and one hyphen
fun checkName(submission: String): Boolean {
    var apostropheFound = false
    var hyphenFound = false
    if (submission == "\'") return false
    if (submission.startsWith('-')) return false
    if (submission.contains("-\'")) return false
    loop@ for (s in submission.replace("\\s".toRegex(), "")) {
        when {
            s.isSurrogate() -> continue@loop
            // Seems like numbers are allowed in certain countries!
            s.isDigit() -> continue@loop
            s.isLetter() -> continue@loop
            s == '-' && !hyphenFound -> hyphenFound = true
            s == '\'' && !apostropheFound -> apostropheFound = true
            else -> return false
        }
    }
    return true
}