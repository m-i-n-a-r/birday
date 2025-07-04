package com.minar.birday.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment.Companion.createInstance
import com.github.appintro.model.SliderPagerBuilder
import com.minar.birday.R


class WelcomeActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showIntroSlides()
        hideSystemUi()
    }

    private fun hideSystemUi() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val navBars = WindowInsetsCompat.Type.navigationBars()
        insetsController.hide(navBars)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        startActivity(Intent(this, MainActivity::class.java))
    }

    // Build and show the slides
    private fun showIntroSlides() {
        val pageOne = SliderPagerBuilder()
            .title(getString(R.string.slide_one_title))
            .description(getString(R.string.slide_one_description))
            .imageDrawable(R.drawable.slide_one)
            .backgroundColorRes(R.color.slideOne)
            .build()
        val pageTwo = SliderPagerBuilder()
            .title(getString(R.string.slide_two_title))
            .description(getString(R.string.slide_two_description))
            .imageDrawable(R.drawable.slide_two)
            .backgroundColorRes(R.color.slideTwo)
            .build()
        val pageThree = SliderPagerBuilder()
            .title(getString(R.string.slide_three_title))
            .description(getString(R.string.slide_three_description))
            .imageDrawable(R.drawable.slide_three)
            .backgroundColorRes(R.color.slideThree)
            .build()

        // Options
        isSkipButtonEnabled = false
        showStatusBar(false)
        vibrateDuration = 30
        isColorTransitionsEnabled = true

        addSlide(createInstance(pageOne))
        addSlide(createInstance(pageTwo))
        addSlide(createInstance(pageThree))
    }
}