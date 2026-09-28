package com.minar.birday.fragments

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.preferences.PreferenceTilesDecoration
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.LocalDateTypeConverter
import com.minar.birday.utilities.addNavbarClearance
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.utilities.isProgressiveBlurAvailable
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidgetProvider
import com.minar.birday.widgets.MinimalWidgetProvider


class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Applied live, no restart needed
        findPreference<SwitchPreferenceCompat>("hide_scroll")?.setOnPreferenceChangeListener { _, value ->
            (activity as? MainActivity)?.applyNavbarHideOnScroll(value as Boolean)
            true
        }

        val experimentalPreference: Preference? = findPreference("experimental")
        experimentalPreference?.setOnPreferenceClickListener {
            val navController: NavController =
                findNavController()
            navController.navigate(R.id.action_navigationSettings_to_experimentalSettingsFragment)
            true
        }

        // The progressive blur needs a runtime shader, so below Android 13 the option is hidden
        // instead of sitting there doing nothing
        val blurPreference: SwitchPreferenceCompat? = findPreference("edge_blur")
        if (!isProgressiveBlurAvailable) blurPreference?.isVisible = false
        else blurPreference?.setOnPreferenceChangeListener { _, newValue ->
            (activity as? MainActivity)?.applyEdgeBlur(newValue as Boolean)
            true
        }

        // Set a custom summary provider for the multi selection option
        val additionalNotificationPref =
            findPreference<MultiSelectListPreference>("multi_additional_notification")
        additionalNotificationPref?.summaryProvider =
            Preference.SummaryProvider<MultiSelectListPreference> { preference ->
                val selectedOptions = preference.values
                generateMultiSelectSummary(
                    selectedOptions,
                    getString(R.string.additional_notification_description)
                )
            }
    }

    override fun onResume() {
        super.onResume()
        // Set up a listener whenever a key changes
        preferenceScreen.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listener whenever a key changes
        preferenceScreen.sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (sharedPreferences == null) return
        when (key) {
            "theme_color" -> {
                // The activity should be refreshed automatically when the main theme changes,
                // so there's no point in using a custom approach
                sharedPreferences.edit { putBoolean("refreshed", true) }
                when (sharedPreferences.getString("theme_color", "")) {
                    "dark" -> {
                        // Needed because passing from Amoled to dark doesn't trigger the refresh
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        hotReloadActivity(sharedPreferences)
                    }

                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    // Else means system, or unexpected (and impossible) case
                    else -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        hotReloadActivity(sharedPreferences)
                    }
                }
            }

            "accent_color" -> hotReloadActivity(sharedPreferences)
            "amoled_dark" -> hotReloadActivity(sharedPreferences)
            "shimmer" -> hotReloadActivity(sharedPreferences)
            "notification_hour" -> mainViewModel.scheduleNextCheck()
            "notification_minute" -> mainViewModel.scheduleNextCheck()
            "surname_first" -> updateWidgets(updateUpcoming = true, updateMinimal = true)
            "hide_images" -> updateWidgets(updateUpcoming = true)
            "multi_additional_notification" -> updateWidgets(updateMinimal = true)
            "disable_astrology" -> (requireActivity() as MainActivity).forceRefreshStats()
            // The dao is already bound here, so the flag is set directly instead of going through
            // EventDatabase, then the list is reloaded to reproject the Feb 29 events
            LocalDateTypeConverter.PREFERENCE_KEY -> {
                LocalDateTypeConverter.loadPreference(requireContext())
                mainViewModel.refreshEvents()
            }
        }
    }

    // Reload the activity and make sure to stay in the settings
    private fun hotReloadActivity(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit { putBoolean("refreshed", true) }
        // Recreate doesn't support an animation, but any workaround is buggy
        val activity = requireActivity()
        ActivityCompat.recreate(activity)
    }

    // Refresh one or more widgets
    private fun updateWidgets(updateUpcoming: Boolean = false, updateMinimal: Boolean = false) {
        // Update every existing widget with a broadcast
        if (updateUpcoming) {
            val upcomingIntent = Intent(context, EventWidgetProvider::class.java)
            upcomingIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val upcomingIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(requireContext(), EventWidgetProvider::class.java)
            )
            upcomingIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, upcomingIds)
            requireContext().sendBroadcast(upcomingIntent)
        }
        if (updateMinimal) {
            val minimalIntent = Intent(context, MinimalWidgetProvider::class.java)
            minimalIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val minimalIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(requireContext(), MinimalWidgetProvider::class.java)
            )
            minimalIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, minimalIds)
            requireContext().sendBroadcast(minimalIntent)
        }
    }

    // Generate the summary for a multi select preference (not done by default)
    private fun generateMultiSelectSummary(
        selectedValues: Set<String>?,
        currentSummary: String
    ): String {
        if (selectedValues.isNullOrEmpty()) return currentSummary.replace("%s", "")
        val sortedValues = selectedValues.toMutableList()
        sortedValues.sortBy { it.toInt() }
        var formattedValues = ""
        for (value in sortedValues) {
            formattedValues += if (sortedValues.lastOrNull() == value)
                ""
            else
                "$value, "
        }
        val formattedValuesComplete = formattedValues + resources.getQuantityString(
            R.plurals.days_left,
            if (sortedValues.any { it.toInt() == 1 } && sortedValues.size == 1) 1 else 10,
            sortedValues.last().toInt(),
        )
        return currentSummary.replace("%s", formattedValuesComplete)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // A PreferenceFragmentCompat comes with a see through root, and two see through pages
        // sliding over each other during a navigation read as one smeared page
        view.setBackgroundColor(getThemeColor(android.R.attr.colorBackground, requireContext()))

        // Add insets for preferences
        val recyclerView = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)
        recyclerView.clipToPadding = false
        recyclerView.addNavbarClearance()
        // Tiles instead of dividers: the groups already tell the categories apart
        setDivider(null)
        recyclerView.addItemDecoration(PreferenceTilesDecoration(this))
    }
}
