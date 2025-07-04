package com.minar.birday.fragments

import android.os.Bundle
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.utilities.addInsetsByPadding


class ExperimentalSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.experimental_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add insets for preferences
        val recyclerView = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)
        recyclerView.addInsetsByPadding(bottom = true)

        // Manage the predictive back between fragments
        val predictiveBackMargin =
            resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
        var initialTouchY = -1f
        val fragmentBackground = this.view
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    if (fragmentBackground == null) return
                    val progress =
                        MainActivity.GestureInterpolator.getInterpolation(backEvent.progress)
                    if (initialTouchY < 0f) {
                        initialTouchY = backEvent.touchY
                    }
                    val progressY = MainActivity.GestureInterpolator.getInterpolation(
                        (backEvent.touchY - initialTouchY) / fragmentBackground.height
                    )

                    // Shift horizontally
                    val maxTranslationX = (fragmentBackground.width / 20) - predictiveBackMargin
                    fragmentBackground.translationX = progress * maxTranslationX *
                            (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1)

                    // Shift vertically
                    val maxTranslationY =
                        (fragmentBackground.height / 20) - predictiveBackMargin
                    fragmentBackground.translationY = progressY * maxTranslationY

                    // Scale down from 100% to 90%
                    val scale = 1f - (0.1f * progress)
                    fragmentBackground.scaleX = scale
                    fragmentBackground.scaleY = scale
                }

                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }

                override fun handleOnBackCancelled() {
                    if (fragmentBackground == null) return
                    initialTouchY = -1f
                    fragmentBackground.run {
                        translationX = 0f
                        translationY = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                }
            }
        )
    }
}
