package com.minar.birday.activities

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.databinding.ActivityMinimalWidgetConfigurationBinding
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable
import com.minar.birday.utilities.formatEventList
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.nextDateFormatted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class MinimalWidgetConfigurationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMinimalWidgetConfigurationBinding
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var widgetManager: AppWidgetManager
    private lateinit var views: RemoteViews

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Retrieve the shared preferences
        val theme = sharedPrefs.getString("theme_color", "system")
        val accent = sharedPrefs.getString("accent_color", "system")

        // Avoid crashes when the widget is used before opening the app for the very first time
        if (sharedPrefs.getBoolean("first", true)) {
            val editor = sharedPrefs.edit()
            // Set default accent based on the Android version
            when (Build.VERSION.SDK_INT) {
                23, 24, 25, 26, 27, 28, 29 -> editor.putString("accent_color", "blue")
                31 -> editor.putString("accent_color", "system")
                else -> editor.putString("accent_color", "monet")
            }
            editor.apply()
        }

        // Set the base theme and the accent
        when (theme) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
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
        binding = ActivityMinimalWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val doneButton = binding.configurationConfirmButton
        val darkText = binding.configurationDarkTextSwitch
        val background = binding.configurationBackgroundSwitch
        val compact = binding.configurationCompactSwitch
        val alignStart = binding.configurationAlignStartSwitch
        val hideIfFar = binding.configurationHideIfFarSwitch

        // Restore the state of the saved configuration
        val darkTextValue = sharedPrefs.getBoolean("widget_minimal_dark_text", false)
        val backgroundValue = sharedPrefs.getBoolean("widget_minimal_background", false)
        val compactValue = sharedPrefs.getBoolean("widget_minimal_compact", false)
        val alignStartValue = sharedPrefs.getBoolean("widget_minimal_align_start", false)
        val hideIfFarValue = sharedPrefs.getBoolean("widget_hide_if_far", false)

        darkText.isChecked = darkTextValue
        background.isChecked = backgroundValue
        compact.isChecked = compactValue
        alignStart.isChecked = alignStartValue
        hideIfFar.isChecked = hideIfFarValue

        // Animate the title image
        binding.configurationTitleImage.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_nav_settings, 1000
        )

        // Initialize any widget related variable
        widgetManager = AppWidgetManager.getInstance(this)
        views = RemoteViews(this.packageName, R.layout.widget_minimal)
        val hiPadding = resources.getDimension(R.dimen.between_row_padding).toInt()
        val loPadding = resources.getDimension(R.dimen.widget_margin).toInt()
        // Find the widget id from the intent
        val startIntent = intent
        val widgetId = startIntent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Manage the preview and initialize it
        val previewTitleLight = binding.minimalWidgetPreviewTitleLight
        val previewTitleDark = binding.minimalWidgetPreviewTitleDark
        val previewTextLight = binding.minimalWidgetPreviewTextLight
        val previewTextDark = binding.minimalWidgetPreviewTextDark
        val previewBackgroundLight = binding.minimalWidgetPreviewBackgroundLight
        val previewBackgroundDark = binding.minimalWidgetPreviewBackgroundDark

        if (darkTextValue) {
            previewTitleLight.visibility = View.GONE
            previewTextLight.visibility = View.GONE
            if (compact.isChecked) previewTitleDark.visibility = View.GONE
            else previewTitleDark.visibility = View.VISIBLE
            previewTextDark.visibility = View.VISIBLE
            if (background.isChecked) {
                previewBackgroundLight.visibility = View.VISIBLE
                previewBackgroundDark.visibility = View.GONE
            }
        } else {
            if (compact.isChecked) previewTitleLight.visibility = View.GONE
            else previewTitleLight.visibility = View.VISIBLE
            previewTextLight.visibility = View.VISIBLE
            previewTitleDark.visibility = View.GONE
            previewTextDark.visibility = View.GONE
            if (background.isChecked) {
                previewBackgroundLight.visibility = View.GONE
                previewBackgroundDark.visibility = View.VISIBLE
            }
        }
        if (!backgroundValue) {
            previewBackgroundDark.visibility = View.GONE
            previewBackgroundLight.visibility = View.GONE
            previewTitleLight.setPadding(loPadding, 0, loPadding, loPadding)
            previewTextLight.setPadding(loPadding, 0, loPadding, 0)
            previewTitleDark.setPadding(loPadding, 0, loPadding, loPadding)
            previewTextDark.setPadding(loPadding, 0, loPadding, 0)
        } else {
            previewTitleLight.setPadding(hiPadding, loPadding, hiPadding, loPadding)
            previewTextLight.setPadding(hiPadding, 0, hiPadding, loPadding)
            previewTitleDark.setPadding(hiPadding, loPadding, hiPadding, loPadding)
            previewTextDark.setPadding(hiPadding, 0, hiPadding, loPadding)
        }
        if (compactValue) {
            previewTitleDark.visibility = View.GONE
            previewTitleLight.visibility = View.GONE
        }
        if (!alignStartValue) {
            binding.minimalWidgetPreviewLinearLayout.gravity = Gravity.CENTER
            previewTitleDark.gravity = Gravity.CENTER
            previewTitleLight.gravity = Gravity.CENTER
            previewTextDark.gravity = Gravity.CENTER
            previewTextLight.gravity = Gravity.CENTER
        }

        // Dark text preview
        darkText.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                previewTitleLight.visibility = View.GONE
                previewTextLight.visibility = View.GONE
                if (compact.isChecked) previewTitleDark.visibility = View.GONE
                else previewTitleDark.visibility = View.VISIBLE
                previewTextDark.visibility = View.VISIBLE
                if (background.isChecked) {
                    previewBackgroundLight.visibility = View.VISIBLE
                    previewBackgroundDark.visibility = View.GONE
                }
            } else {
                if (compact.isChecked) previewTitleLight.visibility = View.GONE
                else previewTitleLight.visibility = View.VISIBLE
                previewTextLight.visibility = View.VISIBLE
                previewTitleDark.visibility = View.GONE
                previewTextDark.visibility = View.GONE
                if (background.isChecked) {
                    previewBackgroundLight.visibility = View.GONE
                    previewBackgroundDark.visibility = View.VISIBLE
                }
            }
        }

        // Background preview
        background.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!darkText.isChecked) {
                    previewBackgroundLight.visibility = View.GONE
                    previewBackgroundDark.visibility = View.VISIBLE
                } else {
                    previewBackgroundLight.visibility = View.VISIBLE
                    previewBackgroundDark.visibility = View.GONE
                }
            } else {
                previewBackgroundDark.visibility = View.GONE
                previewBackgroundLight.visibility = View.GONE
            }
            if (!isChecked) {
                previewTitleLight.setPadding(loPadding, 0, loPadding, loPadding)
                previewTextLight.setPadding(loPadding, 0, loPadding, 0)
                previewTitleDark.setPadding(loPadding, 0, loPadding, loPadding)
                previewTextDark.setPadding(loPadding, 0, loPadding, 0)
            } else {
                previewTitleLight.setPadding(hiPadding, loPadding, hiPadding, loPadding)
                previewTextLight.setPadding(hiPadding, 0, hiPadding, loPadding)
                previewTitleDark.setPadding(hiPadding, loPadding, hiPadding, loPadding)
                previewTextDark.setPadding(hiPadding, 0, hiPadding, loPadding)
            }
        }

        // Alignment preview
        alignStart.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.minimalWidgetPreviewLinearLayout.gravity = Gravity.START
                previewTitleDark.gravity = Gravity.START
                previewTitleLight.gravity = Gravity.START
                previewTextDark.gravity = Gravity.START
                previewTextLight.gravity = Gravity.START
            } else {
                binding.minimalWidgetPreviewLinearLayout.gravity = Gravity.CENTER
                previewTitleDark.gravity = Gravity.CENTER
                previewTitleLight.gravity = Gravity.CENTER
                previewTextDark.gravity = Gravity.CENTER
                previewTextLight.gravity = Gravity.CENTER
            }
        }

        // Compact preview
        compact.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                previewTitleLight.visibility = View.GONE
                previewTitleDark.visibility = View.GONE
            } else {
                if (!darkText.isChecked) previewTitleLight.visibility = View.VISIBLE
                else previewTitleDark.visibility = View.VISIBLE
            }
        }

        // Collect the options selected
        doneButton.setOnClickListener {
            // Save everything in shared preferences
            val editor = sharedPrefs.edit()
            editor.putBoolean("widget_minimal_dark_text", darkText.isChecked)
            editor.putBoolean("widget_minimal_background", background.isChecked)
            editor.putBoolean("widget_minimal_compact", compact.isChecked)
            editor.putBoolean("widget_minimal_align_start", alignStart.isChecked)
            editor.putBoolean("widget_minimal_hide_if_far", hideIfFar.isChecked)
            editor.apply()

            // Hide the text views and backgrounds depending on light or dark
            val titleTextView: Int
            val textTextView: Int
            if (darkText.isChecked) {
                views.setViewVisibility(R.id.minimalWidgetTitleLight, View.GONE)
                views.setViewVisibility(R.id.minimalWidgetTextLight, View.GONE)
                views.setViewVisibility(R.id.minimalWidgetBackgroundLight, View.VISIBLE)
                views.setViewVisibility(R.id.minimalWidgetTitleDark, View.VISIBLE)
                views.setViewVisibility(R.id.minimalWidgetTextDark, View.VISIBLE)
                views.setViewVisibility(R.id.minimalWidgetBackgroundDark, View.GONE)
                titleTextView = R.id.minimalWidgetTitleDark
                textTextView = R.id.minimalWidgetTextDark
            } else {
                views.setViewVisibility(R.id.minimalWidgetTitleLight, View.VISIBLE)
                views.setViewVisibility(R.id.minimalWidgetTextLight, View.VISIBLE)
                views.setViewVisibility(R.id.minimalWidgetBackgroundLight, View.GONE)
                views.setViewVisibility(R.id.minimalWidgetTitleDark, View.GONE)
                views.setViewVisibility(R.id.minimalWidgetTextDark, View.GONE)
                views.setViewVisibility(R.id.minimalWidgetBackgroundDark, View.VISIBLE)
                titleTextView = R.id.minimalWidgetTitleLight
                textTextView = R.id.minimalWidgetTextLight
            }
            // Align the text to start if selected
            if (alignStart.isChecked) {
                views.setInt(R.id.minimalWidgetLinearLayout, "setGravity", Gravity.START)
                views.setInt(titleTextView, "setGravity", Gravity.START)
                views.setInt(textTextView, "setGravity", Gravity.START)
            } else {
                views.setInt(R.id.minimalWidgetLinearLayout, "setGravity", Gravity.CENTER)
                views.setInt(titleTextView, "setGravity", Gravity.CENTER)
                views.setInt(textTextView, "setGravity", Gravity.CENTER)
            }
            // Set the padding depending on the background
            if (background.isChecked) {
                views.setViewPadding(titleTextView, hiPadding, loPadding, hiPadding, loPadding)
                views.setViewPadding(textTextView, hiPadding, 0, hiPadding, loPadding)
            } else {
                views.setViewPadding(titleTextView, loPadding, loPadding, loPadding, loPadding)
                views.setViewPadding(textTextView, loPadding, 0, loPadding, loPadding)
                views.setViewVisibility(R.id.minimalWidgetBackgroundDark, View.GONE)
                views.setViewVisibility(R.id.minimalWidgetBackgroundLight, View.GONE)
            }
            // Activate the compact layout if selected
            if (compact.isChecked) {
                views.setViewVisibility(titleTextView, View.GONE)
            } else {
                views.setViewVisibility(titleTextView, View.VISIBLE)
            }

            // Update the content
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            val intent = Intent(this, MainActivity::class.java)

            Thread {
                // Get the next events and the proper formatter
                val eventDao: EventDao = EventDatabase.getBirdayDatabase(this).eventDao()
                val nextEvents: List<EventResult> = eventDao.getOrderedNextEventsStatic()

                // Launch the app on click
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                val pendingIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                views.setOnClickPendingIntent(R.id.minimalWidgetMain, pendingIntent)

                // Remove events in the future today (eg: now is december 1st 2023, an event has original date = december 1st 2050)
                var filteredNextEvents = nextEvents.toMutableList()
                filteredNextEvents.removeIf { getNextYears(it) == 0 }
                // If the events are all in the future, display them anyway
                if (filteredNextEvents.isEmpty()) {
                    filteredNextEvents = nextEvents.toMutableList()
                }

                // Make sure to show if there's more than one event
                var widgetUpcoming = formatEventList(filteredNextEvents, true, this, false)
                if (filteredNextEvents.isNotEmpty()) widgetUpcoming += "\n${
                    nextDateFormatted(
                        filteredNextEvents[0], formatter, this
                    )
                }"
                views.setTextViewText(textTextView, widgetUpcoming)

                // Hide the entire widget if the event is far enough in time
                if (hideIfFar.isChecked) {
                    val anticipationDays =
                        sharedPrefs.getString("additional_notification", "0")!!.toInt()
                    if (filteredNextEvents.isEmpty() || LocalDate.now()
                            .until(filteredNextEvents.first().nextDate).days > anticipationDays
                    )
                        views.setViewVisibility(R.id.minimalWidgetMain, View.INVISIBLE)
                    else views.setViewVisibility(R.id.minimalWidgetMain, View.VISIBLE)
                }
                // Instruct the widget manager to update the widget
                widgetManager.updateAppWidget(widgetId, views)
            }.start()

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}