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
import androidx.preference.PreferenceFragmentCompat
import com.minar.birday.R
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidget


@ExperimentalStdlibApi
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
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
            "dark_widget" -> {
                // Update every existing widget with a broadcast
                val intent = Intent(context, EventWidget::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(requireContext(), EventWidget::class.java)
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
        ActivityCompat.recreate(requireActivity())
    }

}
