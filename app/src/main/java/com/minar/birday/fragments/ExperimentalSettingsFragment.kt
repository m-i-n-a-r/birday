package com.minar.birday.fragments

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.utilities.addNavbarClearance
import com.minar.birday.utilities.getThemeColor


class ExperimentalSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)
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
    }
}
