package com.minar.birday.utilities

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
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
// Amoled used to be a theme value on its own, it is a switch over the dark one now
private fun SharedPreferences.migrateAmoledTheme() {
    if (getString("theme_color", "system") != "black") return
    edit {
        putString("theme_color", "dark")
        putBoolean("amoled_dark", true)
    }
}

// Pure black applies to whatever ends up being dark, the system theme included
fun SharedPreferences.isAmoledActive(context: Context): Boolean {
    if (!getBoolean("amoled_dark", false)) return false
    return when (getString("theme_color", "system")) {
        "dark" -> true
        "light" -> false
        else -> context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}

fun AppCompatActivity.applyUserTheme(sharedPrefs: SharedPreferences) {
    sharedPrefs.migrateAmoledTheme()
    val theme = sharedPrefs.getString("theme_color", "system")
    val accent = sharedPrefs.getString("accent_color", "system")

    when (theme) {
        "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    val amoled = sharedPrefs.isAmoledActive(this)
    // The base theme is applied first, so the amoled styles only override what they redefine
    if (amoled) setTheme(R.style.AppTheme)
    setTheme(accentThemeRes(accent, perfectDark = amoled))

    // Dynamic colors leave the on*Container roles on the static baseline in light
    if (accent == "monet") setTheme(R.style.ThemeOverlay_App_MonetContainerFix)
}
