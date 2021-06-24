package com.minar.birday.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.model.SliderPagerBuilder
import com.minar.birday.R


class WelcomeActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO wait for a non-deprecated universal solution (insets controller is for API 30 only)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        showIntroSlides()
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