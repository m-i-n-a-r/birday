package com.minar.birday.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.preferences.PreferenceTilesDecoration
import com.minar.birday.utilities.addNavbarClearance
import com.minar.birday.utilities.additionalTimeSeparate
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.viewmodels.MainViewModel


class ExperimentalSettingsFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)
        // The dates follow the calendar, or go back to the Gregorian ones. The listener runs
        // before the value is saved, the refresh right after it
        findPreference<ListPreference>("alternative_calendar")?.setOnPreferenceChangeListener { _, _ ->
            listView.post { mainViewModel.refreshCalendars() }
            true
        }
        showAdditionalTime()
    }

    // The time of the additional notifications only shows while they have one of their own
    private fun showAdditionalTime() {
        findPreference<Preference>("additional_notification")?.isVisible =
            additionalTimeSeparate(requireContext())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "additional_notification_separate" -> {
                showAdditionalTime()
                mainViewModel.scheduleNextCheck()
            }

            "additional_notification_hour", "additional_notification_minute" ->
                mainViewModel.scheduleNextCheck()
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
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
