package com.minar.birday.utilities

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.minar.birday.R
import com.minar.birday.activities.MainActivity

/**
 * Google's predictive back spec applied to a fragment: while the user swipes, [background] scales
 * down to 90%, shifts toward the swiped edge and follows the finger vertically, revealing the
 * destination underneath. Committing pops the back stack, canceling springs everything back.
 *
 * The same block is currently hand written in DetailsFragment, OverviewFragment and
 * ExperimentalSettingsFragment: they can all be moved here.
 */
fun Fragment.setupPredictiveBack(background: View) {
    val predictiveBackMargin = resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
    var initialTouchY = -1f

    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        object : OnBackPressedCallback(true) {

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                val progress = MainActivity.GestureInterpolator.getInterpolation(backEvent.progress)
                if (initialTouchY < 0f) initialTouchY = backEvent.touchY
                val progressY = MainActivity.GestureInterpolator.getInterpolation(
                    (backEvent.touchY - initialTouchY) / background.height
                )

                // Shift horizontally
                val maxTranslationX = (background.width / 20) - predictiveBackMargin
                background.translationX = progress * maxTranslationX *
                        (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1)

                // Shift vertically
                val maxTranslationY = (background.height / 20) - predictiveBackMargin
                background.translationY = progressY * maxTranslationY

                // Scale down from 100% to 90%
                val scale = 1f - (0.1f * progress)
                background.scaleX = scale
                background.scaleY = scale
            }

            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }

            override fun handleOnBackCancelled() {
                initialTouchY = -1f
                background.run {
                    translationX = 0f
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
        }
    )
}
