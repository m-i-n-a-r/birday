package com.minar.birday.fragments

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.minar.birday.R
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidgetProvider


class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val experimentalPreference: Preference? = findPreference("experimental")
        experimentalPreference?.setOnPreferenceClickListener {
            val navController: NavController =
                findNavController()
            navController.navigate(R.id.action_navigationSettings_to_experimentalSettingsFragment)
            true
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "theme_color" -> {
                // The activity should be refreshed automatically when the main theme changes,
                // so there's no point in using a custom approach
                sharedPreferences.edit().putBoolean("refreshed", true).apply()
                when (sharedPreferences.getString("theme_color", "")) {
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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
        }
    }

    // Reload the activity and make sure to stay in the settings
    private fun hotReloadActivity(sharedPreferences: SharedPreferences) {
        sharedPreferences.edit().putBoolean("refreshed", true).apply()
        // Recreate doesn't support an animation, but any workaround is buggy
        val activity = requireActivity()
        ActivityCompat.recreate(activity)
    }

}
