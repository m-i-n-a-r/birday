package com.minar.birday.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.preferences.PreferenceTilesDecoration
import com.minar.birday.utilities.addNavbarClearance
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.viewmodels.MainViewModel


class ExperimentalSettingsFragment : PreferenceFragmentCompat() {

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)
        // The dates follow the calendar, or go back to the Gregorian ones. The listener runs
        // before the value is saved, the refresh right after it
        findPreference<ListPreference>("alternative_calendar")?.setOnPreferenceChangeListener { _, _ ->
            listView.post { mainViewModel.refreshCalendars() }
            true
        }
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
