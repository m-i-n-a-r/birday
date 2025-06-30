package com.minar.birday.utilities

import android.content.SharedPreferences
import androidx.fragment.app.FragmentActivity
import com.minar.birday.activities.MainActivity
import com.minar.birday.fragments.dialogs.RateBottomSheet
import androidx.core.content.edit

object AppRater {

    private const val DO_NOT_SHOW_AGAIN = "do_not_show_again"
    private const val APP_RATING = "app_rating"
    private const val LAUNCH_COUNT = "launch_count"
    private const val DATE_FIRST_LAUNCH = "date_first_launch"
    private const val DAYS_UNTIL_PROMPT = 2 // Min number of days
    private const val LAUNCHES_UNTIL_PROMPT = 3 // Min number of launches

    @JvmStatic
    fun appLaunched(activity: MainActivity) {
        val prefs = activity.getSharedPreferences(APP_RATING, 0)
        if (prefs.getBoolean(DO_NOT_SHOW_AGAIN, false)) return
        prefs.edit {

            // Increment launch counter
            val launchCount = prefs.getLong(LAUNCH_COUNT, 0) + 1
            putLong(LAUNCH_COUNT, launchCount)

            // Get date of first launch
            var dateFirstLaunch = prefs.getLong(DATE_FIRST_LAUNCH, 0)
            if (dateFirstLaunch == 0L) {
                dateFirstLaunch = System.currentTimeMillis()
                putLong(DATE_FIRST_LAUNCH, dateFirstLaunch)
            }

            // Wait at least n days before opening
            if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
                if (System.currentTimeMillis() >= dateFirstLaunch + DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000) {
                    showRateDialog(activity, this)
                }
            }
        }
    }

    private fun showRateDialog(activity: FragmentActivity, editor: SharedPreferences.Editor) {
        val bottomSheet = RateBottomSheet(editor)
        if (bottomSheet.isAdded) return
        bottomSheet.show(activity.supportFragmentManager, "rate_bottom_sheet")
    }
}