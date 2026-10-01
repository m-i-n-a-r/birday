package com.minar.birday.model

import androidx.annotation.DrawableRes

/**
 * One line of the stats sheet. The icon says at a glance what the line is about, and the text is
 * the whole sentence with the values that matter already highlighted inside it.
 */
data class Stat(@DrawableRes val icon: Int, val text: CharSequence)
