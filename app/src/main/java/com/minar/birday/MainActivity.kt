package com.minar.birday

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // getSharedPreferences(MyPrefs, Context.MODE_PRIVATE); retrieves a specific shared preferences file
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = sp.getString("theme_color", "system")
        val accent = sp.getString("accent_color", "blue")

        // Set the base theme and the accent
        if (theme == "system") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (theme == "dark") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        if (theme == "light") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if (accent == "green") setTheme(R.style.AppTheme_green)
        if (accent == "orange") setTheme(R.style.AppTheme_orange)
        if (accent == "yellow") setTheme(R.style.AppTheme_yellow)
        if (accent == "teal") setTheme(R.style.AppTheme_teal)
        if (accent == "violet") setTheme(R.style.AppTheme_violet)
        if (accent == "pink") setTheme(R.style.AppTheme_pink)
        if (accent == "lightBlue") setTheme(R.style.AppTheme_lightBlue)
        if (accent == "red") setTheme(R.style.AppTheme_red)
        if (accent == "lime") setTheme(R.style.AppTheme_lime)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the bottom navigation bar and configure it for the navigation plugin
        val navigation = findViewById<BottomNavigationView>(R.id.navigation)
        val navController: NavController = Navigation.findNavController(this, R.id.navHostFragment)
        navigation.setupWithNavController(navController)

    }

    // Some utility functions, used from every fragment connected to this activity
    fun vibrate() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val vib = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (sp.getBoolean("vibration", true)) // Vibrate if the vibration in options is set to on
            vib.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

}
