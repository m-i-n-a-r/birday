package com.minar.birday.utilities

import android.content.SharedPreferences
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.minar.birday.R

// The accent themes are a flat list of styles, each one with an amoled twin, so the accent
// preference has to be mapped to a style by hand. Doing it here keeps the mapping in a single
// place: adding an accent means adding one line, not editing every activity and the widget service.
@StyleRes
fun accentThemeRes(accent: String?, perfectDark: Boolean): Int = when (accent) {
    "monet" -> if (perfectDark) R.style.AppTheme_Monet_PerfectDark else R.style.AppTheme_Monet
    "system" -> if (perfectDark) R.style.AppTheme_System_PerfectDark else R.style.AppTheme_System
    "brown" -> if (perfectDark) R.style.AppTheme_Brown_PerfectDark else R.style.AppTheme_Brown
    "blue" -> if (perfectDark) R.style.AppTheme_Blue_PerfectDark else R.style.AppTheme_Blue
    "green" -> if (perfectDark) R.style.AppTheme_Green_PerfectDark else R.style.AppTheme_Green
    "orange" -> if (perfectDark) R.style.AppTheme_Orange_PerfectDark else R.style.AppTheme_Orange
    "yellow" -> if (perfectDark) R.style.AppTheme_Yellow_PerfectDark else R.style.AppTheme_Yellow
    "teal" -> if (perfectDark) R.style.AppTheme_Teal_PerfectDark else R.style.AppTheme_Teal
    "violet" -> if (perfectDark) R.style.AppTheme_Violet_PerfectDark else R.style.AppTheme_Violet
    "pink" -> if (perfectDark) R.style.AppTheme_Pink_PerfectDark else R.style.AppTheme_Pink
    "lightBlue" -> if (perfectDark) R.style.AppTheme_LightBlue_PerfectDark else R.style.AppTheme_LightBlue
    "red" -> if (perfectDark) R.style.AppTheme_Red_PerfectDark else R.style.AppTheme_Red
    "lime" -> if (perfectDark) R.style.AppTheme_Lime_PerfectDark else R.style.AppTheme_Lime
    "crimson" -> if (perfectDark) R.style.AppTheme_Crimson_PerfectDark else R.style.AppTheme_Crimson
    // Default (aqua)
    else -> if (perfectDark) R.style.AppTheme_PerfectDark else R.style.AppTheme
}

// Apply the night mode and the accent chosen by the user. Must be called before setContentView(),
// otherwise the views are inflated with the previous theme.
fun AppCompatActivity.applyUserTheme(sharedPrefs: SharedPreferences) {
    val theme = sharedPrefs.getString("theme_color", "system")
    val accent = sharedPrefs.getString("accent_color", "system")

    // Black is the amoled variant of the dark theme, so it forces the night mode as well
    when (theme) {
        "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        "dark", "black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    // The base theme is applied first, so the amoled styles only override what they redefine
    if (theme == "black") setTheme(R.style.AppTheme)
    setTheme(accentThemeRes(accent, perfectDark = theme == "black"))

    // Dynamic colors leave the on*Container roles on the static baseline in light, so Monet gets
    // them pinned back. A fixed accent must not: it carries its own, from its tonal ramp
    if (accent == "monet") setTheme(R.style.ThemeOverlay_App_MonetContainerFix)
}
