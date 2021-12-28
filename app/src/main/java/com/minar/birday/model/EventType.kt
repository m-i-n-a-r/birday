package com.minar.birday.model

import android.content.Context
import com.minar.birday.R

enum class EventType(val codeName: String, val value: Int) {
    BIRTHDAY("birthday", R.string.birthday),
    ANNIVERSARY("anniv", R.string.anniversary),
    DEATH_ANNIVERSARY("d_anniv", R.string.death_anniversary),
    FESTIVITY("festivity", R.string.festivity),
    NAME_DAY("name", R.string.name_day),
    OTHER("other", R.string.other);

    companion object {
        // Return the types as a list of strings
        fun getNames(context: Context): List<String> {
            return values().map {
                context.getString(it.value)
            }
        }
    }
}