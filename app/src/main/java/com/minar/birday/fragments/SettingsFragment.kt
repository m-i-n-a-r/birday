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
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.utilities.addInsetsByPadding
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidgetProvider
import com.minar.birday.widgets.MinimalWidgetProvider


class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val experimentalPreference: Preference? = findPreference("experimental")
        experimentalPreference?.setOnPreferenceClickListener {
            val navController: NavController =
                findNavController()
            navController.navigate(R.id.action_navigationSettings_to_experimentalSettingsFragment)
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
                sharedPreferences.edit().putBoolean("refreshed", true).apply()
                when (sharedPreferences.getString("theme_color", "")) {
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "black" -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        hotReloadActivity(sharedPreferences)
                    }
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }

            "accent_color" -> hotReloadActivity(sharedPreferences)
            "shimmer" -> hotReloadActivity(sharedPreferences)
            "notification_hour" -> mainViewModel.scheduleNextCheck()
            "notification_minute" -> mainViewModel.scheduleNextCheck()
            "surname_first", "hide_images" -> {
                // Update every existing widget with a broadcast
                val intent = Intent(context, EventWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(requireContext(), EventWidgetProvider::class.java)
                )
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                requireContext().sendBroadcast(intent)
            }

            "multi_additional_notification" -> {
                // Update every existing widget with a broadcast
                val intent = Intent(context, MinimalWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(requireContext(), MinimalWidgetProvider::class.java)
                )
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                requireContext().sendBroadcast(intent)
            }
        }
    }

    // Reload the activity and make sure to stay in the settings
    private fun hotReloadActivity(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().putBoolean("refreshed", true).apply()
        // Recreate doesn't support an animation, but any workaround is buggy
        val activity = requireActivity()
        ActivityCompat.recreate(activity)
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

        // Add insets for preferences
        val recyclerView = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)
        recyclerView.addInsetsByPadding(bottom = true)
    }
}
