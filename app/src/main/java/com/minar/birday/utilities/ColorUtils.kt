package com.minar.birday.utilities

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.AttrRes
import com.minar.birday.R

// Return any attr color, to use it programmatically
fun getThemeColor(@AttrRes attrRes: Int, context: Context): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}

// Return a color to maximize the visibility on another color
fun getBestContrast(color: Int, context: Context, alpha: Int = 255, isDark: Boolean? = null): Int {
    // Calculate the perceptive luminance
    val luma =
        (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
    // Return black for bright colors, white for dark colors
    return if (alpha < 80) {
        getThemeColor(R.attr.colorOnSurface, context)
    } else
        if (luma > 0.5) {
            // Brighter color, darker text
            if (isDark != null && isDark) getThemeColor(R.attr.colorOnSurfaceInverse, context)
            if (isDark != null && !isDark) getThemeColor(R.attr.colorOnSurface, context)
            Color.BLACK
        } else {
            // Darker color, brighter text
            if (isDark != null && isDark) getThemeColor(R.attr.colorOnSurface, context)
            if (isDark != null && !isDark) getThemeColor(R.attr.colorOnSurfaceInverse, context)
            Color.WHITE
        }
}