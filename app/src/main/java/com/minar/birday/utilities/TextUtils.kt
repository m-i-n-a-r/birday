package com.minar.birday.utilities

import java.util.*

// Format a name, considering other uppercase letter, multiple words, the apostrophe and inner spaces
@ExperimentalStdlibApi
fun String.smartFixName(forceCapitalize: Boolean = false): String {
    return replace(Regex("(\\s)+"), " ")
        .trim()
        .split(" ").joinToString(" ") { it ->
            if (forceCapitalize) {
                it.lowercase(Locale.ROOT)
                it.replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.ROOT)
                    else it.toString()
                }
            } else it
        }
        .split("'").joinToString("'") { it ->
            if (forceCapitalize) {
                it.lowercase(Locale.ROOT)
                it.replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.ROOT)
                    else it.toString()
                }
            } else it
        }
        .split("-").joinToString("-") { it ->
            if (forceCapitalize) {
                it.lowercase(Locale.ROOT)
                it.replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.ROOT)
                    else it.toString()
                }
            } else it
        }
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