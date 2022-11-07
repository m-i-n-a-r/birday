package com.minar.birday.utilities

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.minar.birday.R
import com.minar.birday.databinding.MinarMonthBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class MinarMonth(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    // Custom attributes
    private var month = 0
    private var hideWeekDays: Boolean
    private var sundayFirst: Boolean
    private var showSnackBars: Boolean

    private var dateWithChosenMonth: LocalDate
    private val cellsList: MutableList<TextView>

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MinarMonth,
            0, 0
        ).apply {
            try {
                month = getInteger(R.styleable.MinarMonth_month, 0)
                hideWeekDays = getBoolean(R.styleable.MinarMonth_hideWeekDays, false)
                sundayFirst = getBoolean(R.styleable.MinarMonth_sundayAsFirstDay, false)
                showSnackBars = getBoolean(R.styleable.MinarMonth_showInfoSnackBars, false)
            } finally {
                recycle()
            }
        }

        val binding = MinarMonthBinding.inflate(LayoutInflater.from(context), this, true)

        // Week days
        val weekDayOne = binding.weekDayOne
        val weekDayTwo = binding.weekDayTwo
        val weekDayThree = binding.weekDayThree
        val weekDayFour = binding.weekDayFour
        val weekDayFive = binding.weekDayFive
        val weekDaySix = binding.weekDaySix
        val weekDaySeven = binding.weekDaySeven

        // Month cells
        val cell1 = binding.monthCell1
        val cell2 = binding.monthCell2
        val cell3 = binding.monthCell3
        val cell4 = binding.monthCell4
        val cell5 = binding.monthCell5
        val cell6 = binding.monthCell6
        val cell7 = binding.monthCell7
        val cell8 = binding.monthCell8
        val cell9 = binding.monthCell9
        val cell10 = binding.monthCell10
        val cell11 = binding.monthCell11
        val cell12 = binding.monthCell12
        val cell13 = binding.monthCell13
        val cell14 = binding.monthCell14
        val cell15 = binding.monthCell15
        val cell16 = binding.monthCell16
        val cell17 = binding.monthCell17
        val cell18 = binding.monthCell18
        val cell19 = binding.monthCell19
        val cell20 = binding.monthCell20
        val cell21 = binding.monthCell21
        val cell22 = binding.monthCell22
        val cell23 = binding.monthCell23
        val cell24 = binding.monthCell24
        val cell25 = binding.monthCell25
        val cell26 = binding.monthCell26
        val cell27 = binding.monthCell27
        val cell28 = binding.monthCell28
        val cell29 = binding.monthCell29
        val cell30 = binding.monthCell30
        val cell31 = binding.monthCell31
        val cell32 = binding.monthCell32
        val cell33 = binding.monthCell33
        val cell34 = binding.monthCell34
        val cell35 = binding.monthCell35
        val cell36 = binding.monthCell36
        val cell37 = binding.monthCell37
        // Create a list of every cell
        cellsList = mutableListOf(
            cell1,
            cell2,
            cell3,
            cell4,
            cell5,
            cell6,
            cell7,
            cell8,
            cell9,
            cell10,
            cell11,
            cell12,
            cell13,
            cell14,
            cell15,
            cell16,
            cell17,
            cell18,
            cell19,
            cell20,
            cell21,
            cell22,
            cell23,
            cell24,
            cell25,
            cell26,
            cell27,
            cell28,
            cell29,
            cell30,
            cell31,
            cell32,
            cell33,
            cell34,
            cell35,
            cell36,
            cell37
        )

        // Set the letters for the week days
        val monday = DayOfWeek.MONDAY
        val tuesday = DayOfWeek.TUESDAY
        val thursday = DayOfWeek.THURSDAY
        val wednesday = DayOfWeek.WEDNESDAY
        val friday = DayOfWeek.FRIDAY
        val saturday = DayOfWeek.SATURDAY
        val sunday = DayOfWeek.SUNDAY
        val locale = Locale.getDefault()
        if (!hideWeekDays) {
            if (!sundayFirst) {
                weekDayOne.text = monday.getDisplayName(TextStyle.NARROW, locale)
                weekDayTwo.text = tuesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayThree.text = wednesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFour.text = thursday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFive.text = friday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySix.text = saturday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySeven.text = sunday.getDisplayName(TextStyle.NARROW, locale)
            } else {
                weekDayOne.text = sunday.getDisplayName(TextStyle.NARROW, locale)
                weekDayTwo.text = monday.getDisplayName(TextStyle.NARROW, locale)
                weekDayThree.text = tuesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFour.text = wednesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFive.text = thursday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySix.text = friday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySeven.text = saturday.getDisplayName(TextStyle.NARROW, locale)
            }
        } else {
            weekDayOne.visibility = View.GONE
            weekDayTwo.visibility = View.GONE
            weekDayThree.visibility = View.GONE
            weekDayFour.visibility = View.GONE
            weekDayFive.visibility = View.GONE
            weekDaySix.visibility = View.GONE
            weekDaySeven.visibility = View.GONE
        }

        // Set the number and name for the month (from range 0-11 to 1-12)
        dateWithChosenMonth = LocalDate.now().withMonth(month + 1).withDayOfMonth(1)
        val firstDayOfWeekForChosenMonth = dateWithChosenMonth.dayOfWeek
        val monthTitle = binding.overviewMonthName
        monthTitle.text =
            dateWithChosenMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        monthTitle.contentDescription =
            dateWithChosenMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

        if (!sundayFirst)
        // Case 1: monday is the first day of the week
            when (firstDayOfWeekForChosenMonth) {
                DayOfWeek.MONDAY -> renderDays(Range(0, 30))
                DayOfWeek.TUESDAY -> renderDays(Range(1, 31))
                DayOfWeek.WEDNESDAY -> renderDays(Range(2, 32))
                DayOfWeek.THURSDAY -> renderDays(Range(3, 33))
                DayOfWeek.FRIDAY -> renderDays(Range(4, 34))
                DayOfWeek.SATURDAY -> renderDays(Range(5, 35))
                DayOfWeek.SUNDAY -> renderDays(Range(6, 36))
                else -> {}
            }
        else
        // Case 2: sunday is the first day of the week
            when (firstDayOfWeekForChosenMonth) {
                DayOfWeek.SUNDAY -> renderDays(Range(0, 30))
                DayOfWeek.MONDAY -> renderDays(Range(1, 31))
                DayOfWeek.TUESDAY -> renderDays(Range(2, 32))
                DayOfWeek.WEDNESDAY -> renderDays(Range(3, 33))
                DayOfWeek.THURSDAY -> renderDays(Range(4, 34))
                DayOfWeek.FRIDAY -> renderDays(Range(5, 35))
                DayOfWeek.SATURDAY -> renderDays(Range(6, 36))
                else -> {}
            }
    }

    // Render the appropriate numbers and hide any useless text view
    private fun renderDays(monthRange: Range<Int>) {
        val min = monthRange.lower
        val max = monthRange.upper

        // Render the month numbers with a leading space for single digit numbers
        for (i in min..max) {
            val dayValue = i - min + 1
            // Manage single digit dates differently
            val dayNumber = if (dayValue <= 9) " $dayValue" else dayValue.toString()
            cellsList[i].text = dayNumber
            // Accessibility related info
            try {
                val correspondingDate =
                    LocalDate.of(dateWithChosenMonth.year, dateWithChosenMonth.month - 1, dayValue)
                val formatter: DateTimeFormatter =
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                cellsList[i].contentDescription = correspondingDate.format(formatter)
            } catch (_: Exception) {
            }
        }
        // Hide unnecessary cells
        if (min != 0)
            for (i in 0 until min) {
                cellsList[i].visibility = View.INVISIBLE
            }
        when (dateWithChosenMonth.month) {
            Month.NOVEMBER, Month.APRIL, Month.JUNE, Month.SEPTEMBER -> {
                for (i in (30 + min) until cellsList.size)
                    cellsList[i].visibility = View.INVISIBLE
            }
            Month.FEBRUARY -> {
                val leapIndex = if (dateWithChosenMonth.isLeapYear) 28 else 27
                for (i in (leapIndex + min) until cellsList.size)
                    cellsList[i].visibility = View.INVISIBLE
            }
            else -> {
                for (i in (31 + min) until cellsList.size)
                    cellsList[i].visibility = View.INVISIBLE
            }
        }

    }

    // Highlight a day in the month using a drawable or a color
    fun highlightDay(
        day: Int,
        color: Int,
        drawable: Drawable? = null,
        makeBold: Boolean = false,
        autoOpacity: Boolean = false,
        inverseTextColorOnDrawable: Boolean = false,
        asForeground: Boolean = false,
    ) {
        // The textview will be hidden if the day doesn't exist in the current month
        for (cell in cellsList) {
            if (cell.text.trim() == day.toString()) {
                if (drawable == null) {
                    cell.setTextColor(color)
                } else {
                    if (asForeground) {
                        cell.foreground = drawable
                        cell.foregroundTintList = ColorStateList.valueOf(color)
                    } else {
                        cell.background = drawable
                        cell.backgroundTintList = ColorStateList.valueOf(color)
                    }
                    if (autoOpacity) cell.alpha = 0.3f
                    if (inverseTextColorOnDrawable) cell.setTextColor(
                        getThemeColor(
                            R.attr.colorOnSurfaceInverse,
                            context
                        )
                    )
                }
                if (makeBold) cell.setTypeface(null, Typeface.BOLD)
                break
            }
        }
    }

    // Dynamically set the first day of the week
    fun setSundayFirst(enable: Boolean) {
        sundayFirst = enable
        invalidate()
        requestLayout()
    }
}