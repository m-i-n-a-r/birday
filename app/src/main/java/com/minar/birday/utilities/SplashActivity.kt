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
        when (theme) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        when (accent) {
            "blue" -> setTheme(R.style.AppTheme_Blue)
            "green" -> setTheme(R.style.AppTheme_Green)
            "orange" -> setTheme(R.style.AppTheme_Orange)
            "yellow" -> setTheme(R.style.AppTheme_Yellow)
            "teal" -> setTheme(R.style.AppTheme_Teal)
            "violet" -> setTheme(R.style.AppTheme_Violet)
            "pink" -> setTheme(R.style.AppTheme_Pink)
            "lightBlue" -> setTheme(R.style.AppTheme_LightBlue)
            "red" -> setTheme(R.style.AppTheme_Red)
            "lime" -> setTheme(R.style.AppTheme_Lime)
        }

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