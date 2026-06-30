package com.minar.birday.utilities

import java.util.*

// Format a name, considering other uppercase letter, multiple words, the apostrophe and inner spaces
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

// Check if the string is written using letters, numbers, emoticons and only particular symbols
fun checkName(submission: String): Boolean {
    var apostropheFound = false
    var hyphenFound = false
    var ampersandFound = false
    var openParFound = false
    var closedParFound = false
    var atFound = false
    var hashFound = false

    if (submission == "\'") return false
    if (submission.startsWith('-')) return false
    if (submission.contains("-\'")) return false
    loop@ for (s in submission.replace("\\s".toRegex(), "")) {
        // Stop when the first invalid character is found
        when {
            // A surely improvable way to support non the red heart and more "ancient" emojis
            Character.isSurrogate(s) -> continue@loop
            s == '♥' -> continue@loop
            s == '❤' -> continue@loop
            s == '☹' -> continue@loop
            s == '☺' -> continue@loop
            s == '️' -> continue@loop
            s == '.' -> continue@loop

            // Seems like numbers are allowed in certain countries!
            s.isDigit() -> continue@loop
            s.isLetter() -> continue@loop
            s == '/' -> continue@loop
            s == '#' && !hashFound -> hashFound = true
            s == '@' && !atFound -> atFound = true
            s == '(' && !openParFound -> openParFound = true
            s == ')' && !closedParFound -> closedParFound = true
            s == '-' && !hyphenFound -> hyphenFound = true
            s == '\'' && !apostropheFound -> apostropheFound = true
            s == '&' && !ampersandFound -> ampersandFound = true
            else -> return false
        }
    }
    return true
}

// Return the max number of additional notification days selected as an int
fun maxNumberOfAdditionalNotificationDays(selectedDays: MutableSet<String>?): Int {
    if (selectedDays == null) return 0
    var maxNumber = 0
    for (numberOfDays in selectedDays) {
        val daysAsNumber = numberOfDays.toInt()
        if (daysAsNumber > maxNumber)
            maxNumber = daysAsNumber
    }
    return maxNumber
}