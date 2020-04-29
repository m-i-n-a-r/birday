package com.minar.birday.utilities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro2
import com.github.paolorotolo.appintro.AppIntro2Fragment
import com.github.paolorotolo.appintro.model.SliderPagerBuilder
import com.minar.birday.MainActivity
import com.minar.birday.R


class WelcomeActivity : AppIntro2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enter immersive mode
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
            .bgColor(getColor(R.color.slideOne))
            .build()
        val pageTwo = SliderPagerBuilder()
            .title(getString(R.string.slide_two_title))
            .description(getString(R.string.slide_two_description))
            .imageDrawable(R.drawable.slide_two)
            .bgColor(getColor(R.color.slideTwo))
            .build()
        val pageThree = SliderPagerBuilder()
            .title(getString(R.string.slide_three_title))
            .description(getString(R.string.slide_three_description))
            .imageDrawable(R.drawable.slide_three)
            .bgColor(getColor(R.color.slideThree))
            .build()

        // Options
        showStatusBar(false)
        showSkipButton(false)
        setNavBarColor("#e4a522") // Equal to the last slide
        setVibrate(true)
        setVibrateIntensity(30)
        setColorTransitionsEnabled(true)
        //askForPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 3)

        addSlide(AppIntro2Fragment.newInstance(pageOne))
        addSlide(AppIntro2Fragment.newInstance(pageTwo))
        addSlide(AppIntro2Fragment.newInstance(pageThree))
    }
}