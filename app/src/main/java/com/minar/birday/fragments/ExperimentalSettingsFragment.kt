package com.minar.birday.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.minar.birday.R


@ExperimentalStdlibApi
class ExperimentalSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)
    }
}
