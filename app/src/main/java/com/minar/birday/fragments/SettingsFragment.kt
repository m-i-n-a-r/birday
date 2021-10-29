package com.minar.birday.fragments

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
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
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Unregister the listener whenever a key changes
        preferenceScreen.sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "theme_color" -> hotReloadActivity(activity)
            "accent_color" -> hotReloadActivity(activity)
            "shimmer" -> hotReloadActivity(activity)
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
    private fun hotReloadActivity(activity: Activity?) {
        if (activity == null) return
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        sharedPrefs.edit().putBoolean("refreshed", true).apply()
        activity.recreate()
    }

}
