package com.minar.birday.utilities

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import com.minar.birday.R
import com.minar.birday.databinding.MinarYearBinding
import com.minar.birday.model.EventResult
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*

class MinarYear(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    // Custom attributes
    private var hideWeekDays: Boolean
    private var sundayFirst: Boolean
    private var showSnackBars: Boolean
    private var showAdvancedInfo: Boolean
    private var appearance: Int

    private var year: Int = LocalDate.now().year
    private lateinit var monthList: MutableList<MinarMonth>
    private var binding: MinarYearBinding

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.MinarYear, 0, 0
        ).apply {
            try {
                hideWeekDays = getBoolean(R.styleable.MinarYear_hideWeekDays, false)
                sundayFirst = getBoolean(R.styleable.MinarYear_sundayAsFirstDay, false)
                showSnackBars = getBoolean(R.styleable.MinarYear_showInfoSnackBars, true)
                showAdvancedInfo = getBoolean(R.styleable.MinarYear_showAdvancedInfo, false)
                appearance = getInteger(R.styleable.MinarYear_appearance, 0)
            } finally {
                recycle()
            }
        }
        binding = MinarYearBinding.inflate(LayoutInflater.from(context), this, true)
        initYear()
    }

    // Initialize the month
    private fun initYear() {
        // Months
        val january = binding.overviewYearJan
        val february = binding.overviewYearFeb
        val march = binding.overviewYearMar
        val april = binding.overviewYearApr
        val may = binding.overviewYearMay
        val june = binding.overviewYearJun
        val july = binding.overviewYearJul
        val august = binding.overviewYearAug
        val september = binding.overviewYearSep
        val october = binding.overviewYearOct
        val november = binding.overviewYearNov
        val december = binding.overviewYearDec

        // Create a list of every month
        monthList = mutableListOf(
            january,
            february,
            march,
            april,
            may,
            june,
            july,
            august,
            september,
            october,
            november,
            december
        )

        // If sunday is the first day, apply this
        if (WeekFields.of(Locale.getDefault()).firstDayOfWeek.name == "SUNDAY") {
            for (month in monthList) {
                month.setSundayFirst(true)
            }
        }

        // Set the appearance (0 small default, 1 medium, 2 large, 3 xlarge)
        when (appearance) {
            0 -> return
            1 -> setAppearance(1)
            2 -> setAppearance(2)
            3 -> setAppearance(3)
        }
    }

    // Set a specific year for the overview screen
    fun renderYear(year: Int, events: List<EventResult>?) {
        this.year = year
        for (month in monthList) {
            month.setYear(year)
            month.resetHighlighting()
        }
        // Highlight the dates (only if they exist in the current year)
        if (year == LocalDate.now().year) highlightCurrentDate()
        if (events != null && events.isNotEmpty()) {
            var currentDate = events[0].originalDate
            var dayEvents = mutableListOf<EventResult>()
            for (event in events) {
                // Compute the snackbar text if enabled (and the list is ordered)
                if (showAdvancedInfo) {
                    if (event.originalDate.isEqual(currentDate.withYear(event.originalDate.year))) {
                        dayEvents.add(event)
                    } else {
                        dayEvents = mutableListOf()
                        dayEvents.add(event)
                        currentDate = event.originalDate
                    }
                }
                if (event.originalDate.year <= year)
                    highlightDate(
                        event.nextDate,
                        getThemeColor(R.attr.colorPrimary, context),
                        AppCompatResources.getDrawable(context, R.drawable.minar_month_circle),
                        makeBold = false,
                        autoOpacity = true,
                        autoTextColor = true,
                        snackbarText = if (showAdvancedInfo) formatEventList(
                            dayEvents,
                            false,
                            context,
                            false
                        ) else ""
                    )
            }
        }
    }

    // Highlight a date in a year, delegating the highlight to the correct month
    private fun highlightDate(
        date: LocalDate?,
        color: Int,
        drawable: Drawable?,
        makeBold: Boolean = false,
        autoOpacity: Boolean = false,
        autoTextColor: Boolean = false,
        asForeground: Boolean = false,
        snackbarText: String = ""
    ) {
        if (date == null) return
        monthList[date.month.value - 1].highlightDay(
            date.dayOfMonth,
            color,
            drawable,
            makeBold = makeBold,
            autoOpacity = autoOpacity,
            autoTextColor = autoTextColor,
            asForeground = asForeground,
            snackbarText = snackbarText
        )
    }

    // Highlight the current date with a ring
    private fun highlightCurrentDate(drawable: Drawable? = null, color: Int? = null) {
        val date = LocalDate.now()
        if (date.year != year) return
        val chosenColor = color ?: getThemeColor(R.attr.colorTertiary, context)
        val chosenDrawable =
            drawable ?: AppCompatResources.getDrawable(context, R.drawable.minar_month_ring)
        highlightDate(date, chosenColor, chosenDrawable, asForeground = true)
    }

    // Enable additional snack bars
    fun setAdvancedInfoEnabled(enabled: Boolean) {
        showAdvancedInfo = enabled
    }

    // Set the appearance of the entire year, returns the current appearance
    fun setAppearance(appearance: Int): Int {
        if (appearance > 3 || appearance < 0) {
            this.appearance += 1
            if (this.appearance == 4) this.appearance = 0
        } else
            this.appearance = appearance
        for (month in monthList) {
            month.setAppearance(this.appearance)
        }
        return this.appearance
    }
}