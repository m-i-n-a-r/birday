package com.minar.birday.activities

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.model.SliderPagerBuilder
import com.minar.birday.R


@ExperimentalStdlibApi
class WelcomeActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()
        showIntroSlides()
    }

    private fun hideSystemUi() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val navBars = WindowInsetsCompat.Type.navigationBars()
        insetsController.hide(navBars)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
            .backgroundColor(getColor(R.color.slideOne))
            .build()
        val pageTwo = SliderPagerBuilder()
            .title(getString(R.string.slide_two_title))
            .description(getString(R.string.slide_two_description))
            .imageDrawable(R.drawable.slide_two)
            .backgroundColor(getColor(R.color.slideTwo))
            .build()
        val pageThree = SliderPagerBuilder()
            .title(getString(R.string.slide_three_title))
            .description(getString(R.string.slide_three_description))
            .imageDrawable(R.drawable.slide_three)
            .backgroundColor(getColor(R.color.slideThree))
            .build()

        // Options
        showStatusBar(false)
        isSkipButtonEnabled = false
        setNavBarColor(R.color.slideThree)
        vibrateDuration = 30
        isColorTransitionsEnabled = true

        addSlide(AppIntroFragment.newInstance(pageOne))
        addSlide(AppIntroFragment.newInstance(pageTwo))
        addSlide(AppIntroFragment.newInstance(pageThree))
    }
}