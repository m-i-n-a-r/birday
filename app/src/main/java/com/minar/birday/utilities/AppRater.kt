package com.minar.birday.utilities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.minar.birday.R

object AppRater {

    private const val DO_NOT_SHOW_AGAIN = "do_not_show_again"
    private const val APP_RATING = "app_rating"
    private const val LAUNCH_COUNT = "launch_count"
    private const val DATE_FIRST_LAUNCH = "date_first_launch"

    private const val DAYS_UNTIL_PROMPT = 2 // Min number of days
    private const val LAUNCHES_UNTIL_PROMPT = 3 // Min number of launches

    private const val DIALOG_CORNER = 16.toFloat()

    @JvmStatic
    fun appLaunched(context: Context) {
        val prefs = context.getSharedPreferences(APP_RATING, 0)
        if (prefs.getBoolean(DO_NOT_SHOW_AGAIN, false)) return
        val editor = prefs.edit()

        // Increment launch counter
        val launchCount = prefs.getLong(LAUNCH_COUNT, 0) + 1
        editor.putLong(LAUNCH_COUNT, launchCount)

        // Get date of first launch
        var dateFirstLaunch = prefs.getLong(DATE_FIRST_LAUNCH, 0)
        if (dateFirstLaunch == 0L) {
            dateFirstLaunch = System.currentTimeMillis()
            editor.putLong(DATE_FIRST_LAUNCH, dateFirstLaunch)
        }

        // Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= dateFirstLaunch + DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000) {
                showRateDialog(context, editor)
            }
        }
        editor.apply()
    }

    private fun showRateDialog(context: Context, editor: SharedPreferences.Editor) {
        MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT))
            .show {
                cornerRadius(DIALOG_CORNER)
                title(R.string.leave_review_title)
                icon(R.drawable.ic_pen_24dp)
                message(R.string.leave_review_subtitle)
                positiveButton(R.string.yes_review) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}")
                        )
                    )
                    editor.putBoolean(DO_NOT_SHOW_AGAIN, true)
                    editor.commit()
                    dismiss()
                }
                negativeButton(R.string.later) {
                    dismiss()
                }
                @Suppress("DEPRECATION")
                neutralButton(R.string.no_thanks) {
                    editor.putBoolean(DO_NOT_SHOW_AGAIN, true)
                    editor.commit()
                    dismiss()
                }
            }
    }
}