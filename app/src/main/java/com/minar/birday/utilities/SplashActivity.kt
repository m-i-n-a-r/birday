package com.minar.birday.utilities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.minar.birday.MainActivity
import com.minar.birday.R
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = sp.getString("theme_color", "system")
        val accent = sp.getString("accent_color", "brown")

        // Set the base theme and the accent
        if (theme == "system") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (theme == "dark") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        if (theme == "light") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)


        if (accent == "blue") setTheme(R.style.Splash_Blue)
        if (accent == "green") setTheme(R.style.Splash_Green)
        if (accent == "orange") setTheme(R.style.Splash_Orange)
        if (accent == "yellow") setTheme(R.style.Splash_Yellow)
        if (accent == "teal") setTheme(R.style.Splash_Teal)
        if (accent == "violet") setTheme(R.style.Splash_Violet)
        if (accent == "pink") setTheme(R.style.Splash_Pink)
        if (accent == "lightBlue") setTheme(R.style.Splash_LightBlue)
        if (accent == "red") setTheme(R.style.Splash_Red)
        if (accent == "lime") setTheme(R.style.Splash_Lime)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        activityScope.launch {
            delay(1000)

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        activityScope.cancel()
        super.onPause()
    }
}