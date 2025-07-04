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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.databinding.ActivityMinimalWidgetConfigurationBinding
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.addInsetsByPadding
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable
import com.minar.birday.utilities.formatEventList
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.maxNumberOfAdditionalNotificationDays
import com.minar.birday.utilities.nextDateFormatted
import com.minar.birday.utilities.removeOrGetUpcomingEvents
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
        val avdLooping = sharedPrefs.getBoolean("loop_avd", true)
        val surnameFirst = sharedPrefs.getBoolean("surname_first", false)

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
        binding = ActivityMinimalWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.container.addInsetsByPadding(top = true, bottom = true, left = true, right = true)
        val doneButton = binding.configurationConfirmButton
        val darkText = binding.configurationDarkTextSwitch
        val background = binding.configurationBackgroundSwitch
        val compact = binding.configurationCompactSwitch
        val alignStart = binding.configurationAlignStartSwitch
        val hideIfFar = binding.configurationHideIfFarSwitch
        val onlyFavorites = binding.configurationShowOnlyFavoritesSwitch
        val showFollowing = binding.configurationShowFollowingSwitch

        // Restore the state of the saved configuration
        val darkTextValue = sharedPrefs.getBoolean("widget_minimal_dark_text", false)
        val backgroundValue = sharedPrefs.getBoolean("widget_minimal_background", false)
        val compactValue = sharedPrefs.getBoolean("widget_minimal_compact", false)
        val alignStartValue = sharedPrefs.getBoolean("widget_minimal_align_start", false)
        val hideIfFarValue = sharedPrefs.getBoolean("widget_minimal_hide_if_far", false)
        val onlyFavoritesValue = sharedPrefs.getBoolean("widget_minimal_only_favorites", false)
        val showFollowingValue = sharedPrefs.getBoolean("widget_minimal_show_following", false)

        darkText.isChecked = darkTextValue
        background.isChecked = backgroundValue
        compact.isChecked = compactValue
        alignStart.isChecked = alignStartValue
        hideIfFar.isChecked = hideIfFarValue
        onlyFavorites.isChecked = onlyFavoritesValue
        showFollowing.isChecked = showFollowingValue

        // Animate the title image
        binding.configurationTitleImage.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_nav_settings, 1000, disableLooping = !avdLooping
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
        if (showFollowingValue) {
            val showFollowingText =
                "${getString(R.string.no_next_event)} \n${getString(R.string.next_event)} → ${
                    getString(R.string.no_next_event)
                }"
            previewTextDark.text = showFollowingText
            previewTextLight.text = showFollowingText
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

        // Show following event
        showFollowing.setOnCheckedChangeListener { _, isChecked ->
            val showFollowingText =
                "${getString(R.string.no_next_event)} \n${getString(R.string.next_event)} → ${
                    getString(R.string.no_next_event)
                }"
            if (isChecked) {
                previewTextDark.text = showFollowingText
                previewTextLight.text = showFollowingText
            } else {
                previewTextDark.text = getString(R.string.no_next_event)
                previewTextLight.text = getString(R.string.no_next_event)
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
            editor.putBoolean("widget_minimal_only_favorites", onlyFavorites.isChecked)
            editor.putBoolean("widget_minimal_show_following", showFollowing.isChecked)
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
            } else {
                views.setInt(R.id.minimalWidgetLinearLayout, "setGravity", Gravity.CENTER)
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
                val orderedEvents: List<EventResult> = eventDao.getOrderedEventsStatic()

                // Launch the app on click
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                val pendingIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                views.setOnClickPendingIntent(R.id.minimalWidgetMain, pendingIntent)

                // Remove events in the future today (eg: now is december 1st 2023, an event has original date = december 1st 2050)
                var filteredNextEvents =
                    removeOrGetUpcomingEvents(
                        orderedEvents,
                        true,
                        onlyFavorites = onlyFavorites.isChecked
                    ).toMutableList()
                filteredNextEvents.removeIf { getNextYears(it) == 0 }
                // If the events are all in the future, display them
                if (filteredNextEvents.isEmpty()) {
                    filteredNextEvents =
                        removeOrGetUpcomingEvents(
                            orderedEvents,
                            true,
                            onlyFavorites = onlyFavorites.isChecked
                        ).toMutableList()
                }

                // Make sure to show if there's more than one event
                var widgetUpcoming = formatEventList(
                    filteredNextEvents,
                    surnameFirst,
                    this,
                    filteredNextEvents.size == 1
                )
                if (filteredNextEvents.isNotEmpty()) widgetUpcoming += "\n${
                    nextDateFormatted(
                        filteredNextEvents[0], formatter, this
                    )
                }"
                // Show the following event if show following is enabled
                if (showFollowing.isChecked) {
                    var filteredUpcomingEvents =
                        removeOrGetUpcomingEvents(
                            orderedEvents,
                            false,
                            onlyFavorites = onlyFavorites.isChecked
                        ).toMutableList()
                    filteredUpcomingEvents =
                        removeOrGetUpcomingEvents(
                            filteredUpcomingEvents,
                            true,
                            onlyFavorites = onlyFavorites.isChecked
                        ).toMutableList()
                    val widgetUpcomingExpanded =
                        "$widgetUpcoming \n${getString(R.string.next_event)} → ${
                            formatEventList(
                                filteredUpcomingEvents,
                                true,
                                this,
                                false,
                            )
                        }"
                    views.setTextViewText(textTextView, widgetUpcomingExpanded)
                } else
                    views.setTextViewText(textTextView, widgetUpcoming)

                // Hide the entire widget if the event is far enough in time
                if (hideIfFar.isChecked) {
                    val anticipationDays = maxNumberOfAdditionalNotificationDays(
                        sharedPrefs.getStringSet(
                            "multi_additional_notification",
                            setOf()
                        )
                    )
                    if (filteredNextEvents.isEmpty() || LocalDate.now()
                            .until(filteredNextEvents.first().nextDate).days > anticipationDays
                    )
                        views.setViewVisibility(R.id.minimalWidgetMain, View.INVISIBLE)
                    else views.setViewVisibility(R.id.minimalWidgetMain, View.VISIBLE)
                } else views.setViewVisibility(R.id.minimalWidgetMain, View.VISIBLE)

                // Instruct the widget manager to update the widget
                widgetManager.updateAppWidget(widgetId, views)
            }.start()

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}