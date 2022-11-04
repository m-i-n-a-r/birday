package com.minar.birday.utilities

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

// Return any attr color, to use it programmatically
fun getThemeColor(@AttrRes attrRes: Int, context: Context): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}