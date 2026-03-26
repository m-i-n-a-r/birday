package com.minar.birday.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.databinding.ActivityCompactWidgetConfigurationBinding
import com.minar.birday.utilities.addInsetsByPadding
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable
import com.minar.birday.widgets.CompactWidgetProvider
import androidx.core.content.edit


class CompactWidgetConfigurationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompactWidgetConfigurationBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Retrieve the shared preferences
        val theme = sharedPrefs.getString("theme_color", "system")
        val accent = sharedPrefs.getString("accent_color", "system")
        val avdLooping = sharedPrefs.getBoolean("loop_avd", true)

        // Avoid crashes when the widget is used before opening the app for the very first time
        if (sharedPrefs.getBoolean("first", true)) {
            sharedPrefs.edit {
                // Set default accent based on the Android version
                when (Build.VERSION.SDK_INT) {
                    23, 24, 25, 26, 27, 28, 29 -> putString("accent_color", "blue")
                    31 -> putString("accent_color", "system")
                    else -> putString("accent_color", "monet")
                }
            }
        }

        // Set the base theme and the accent
        when (theme) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "dark", "black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Set an amoled theme or a normal theme depending on amoled mode
        if (theme == "black") {
            setTheme(R.style.AppTheme)
            when (accent) {
                "monet" -> setTheme(R.style.AppTheme_Monet_PerfectDark)
                "system" -> setTheme(R.style.AppTheme_System_PerfectDark)
                "brown" -> setTheme(R.style.AppTheme_Brown_PerfectDark)
                "blue" -> setTheme(R.style.AppTheme_Blue_PerfectDark)
                "green" -> setTheme(R.style.AppTheme_Green_PerfectDark)
                "orange" -> setTheme(R.style.AppTheme_Orange_PerfectDark)
                "yellow" -> setTheme(R.style.AppTheme_Yellow_PerfectDark)
                "teal" -> setTheme(R.style.AppTheme_Teal_PerfectDark)
                "violet" -> setTheme(R.style.AppTheme_Violet_PerfectDark)
                "pink" -> setTheme(R.style.AppTheme_Pink_PerfectDark)
                "lightBlue" -> setTheme(R.style.AppTheme_LightBlue_PerfectDark)
                "red" -> setTheme(R.style.AppTheme_Red_PerfectDark)
                "lime" -> setTheme(R.style.AppTheme_Lime_PerfectDark)
                "crimson" -> setTheme(R.style.AppTheme_Crimson_PerfectDark)
                else -> setTheme(R.style.AppTheme_PerfectDark)
            }
        } else
            when (accent) {
                "monet" -> setTheme(R.style.AppTheme_Monet)
                "system" -> setTheme(R.style.AppTheme_System)
                "brown" -> setTheme(R.style.AppTheme_Brown)
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
                "crimson" -> setTheme(R.style.AppTheme_Crimson)
                else -> setTheme(R.style.AppTheme) // Default (aqua)
            }

        // Initialize the binding
        binding = ActivityCompactWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.container.addInsetsByPadding(top = true, bottom = true, left = true, right = true)

        // Find the widget id from the intent
        val startIntent = intent
        val widgetId = startIntent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Animate the title image
        binding.configurationTitleImage.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_nav_settings, 1000, disableLooping = !avdLooping
        )

        // Restore the saved opacity value (default 80%)
        val savedOpacity = sharedPrefs.getInt("widget_compact_opacity", 80)
        val slider = binding.configurationOpacitySlider
        val opacityValue = binding.configurationOpacityValue
        val previewBackground = binding.compactWidgetPreviewBackground

        slider.value = savedOpacity.toFloat()
        opacityValue.text = "$savedOpacity%"
        updatePreviewBackground(previewBackground, savedOpacity)

        // Restore the saved show photos value (default true)
        val showPhotos = binding.configurationShowPhotosSwitch
        val savedShowPhotos = !sharedPrefs.getBoolean("widget_compact_hide_images", false)
        showPhotos.isChecked = savedShowPhotos

        // Preview avatars
        val previewAvatars = listOf(
            binding.previewAvatar1,
            binding.previewAvatar2,
            binding.previewAvatar3,
        )
        val avatarVisibility = if (savedShowPhotos) android.view.View.VISIBLE else android.view.View.GONE
        previewAvatars.forEach { it.visibility = avatarVisibility }

        // Toggle preview avatars when switch changes
        showPhotos.setOnCheckedChangeListener { _, isChecked ->
            val visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            previewAvatars.forEach { it.visibility = visibility }
        }

        // Update preview when slider changes
        slider.addOnChangeListener { _, value, _ ->
            val opacity = value.toInt()
            opacityValue.text = "$opacity%"
            updatePreviewBackground(previewBackground, opacity)
        }

        // Confirm button
        binding.configurationConfirmButton.setOnClickListener {
            val opacity = slider.value.toInt()
            sharedPrefs.edit {
                putInt("widget_compact_opacity", opacity)
                putBoolean("widget_compact_hide_images", !showPhotos.isChecked)
            }

            // Trigger widget update
            val updateIntent = Intent(this, CompactWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    intArrayOf(widgetId)
                )
            }
            sendBroadcast(updateIntent)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    private fun updatePreviewBackground(view: android.view.View, opacityPercent: Int) {
        val alpha = (opacityPercent * 255 / 100)
        view.setBackgroundColor(Color.argb(alpha, 0, 0, 0))
    }
}
