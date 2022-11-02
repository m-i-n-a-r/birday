package com.minar.birday.utilities

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import com.minar.birday.R
import com.minar.birday.databinding.MinarMonthBinding
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

class MinarMonth(context: Context, attrs: AttributeSet) : GridLayout(context, attrs) {
    // Custom attributes
    private var month = 0
    private var hideWeekDays = false
    private var sundayFirst = false
    private var showSnackBars = false

    init {
        inflate(context, R.layout.minar_month, this)
        val root = MinarMonthBinding.inflate(LayoutInflater.from(context), this, false)

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

        // Week days
        val weekDayOne = root.weekDayOne
        val weekDayTwo = root.weekDayTwo
        val weekDayThree = root.weekDayThree
        val weekDayFour = root.weekDayFour
        val weekDayFive = root.weekDayFive
        val weekDaySix = root.weekDaySix
        val weekDaySeven = root.weekDaySeven

        // Month cells
        var cell1 = root.monthCell1
        var cell2 = root.monthCell2
        var cell3 = root.monthCell3
        var cell4 = root.monthCell4
        var cell5 = root.monthCell5
        var cell6 = root.monthCell6
        var cell7 = root.monthCell7
        var cell8 = root.monthCell8
        var cell9 = root.monthCell9
        var cell10 = root.monthCell10
        var cell11 = root.monthCell11
        var cell12 = root.monthCell12
        var cell13 = root.monthCell13
        var cell14 = root.monthCell14
        var cell15 = root.monthCell15
        var cell16 = root.monthCell16
        var cell17 = root.monthCell17
        var cell18 = root.monthCell18
        var cell19 = root.monthCell19
        var cell20 = root.monthCell20
        var cell21 = root.monthCell21
        var cell22 = root.monthCell22
        var cell23 = root.monthCell23
        var cell24 = root.monthCell24
        var cell25 = root.monthCell25
        var cell26 = root.monthCell26
        var cell27 = root.monthCell27
        var cell28 = root.monthCell28
        var cell29 = root.monthCell29
        var cell30 = root.monthCell30
        var cell31 = root.monthCell31
        var cell32 = root.monthCell32
        var cell33 = root.monthCell33
        var cell34 = root.monthCell34
        var cell35 = root.monthCell35

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
            if (sundayFirst) {
                weekDayOne.text = monday.getDisplayName(TextStyle.NARROW, locale)
                weekDayTwo.text = tuesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayThree.text = thursday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFour.text = wednesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFive.text = friday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySix.text = saturday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySeven.text = sunday.getDisplayName(TextStyle.NARROW, locale)
            } else {
                weekDayOne.text = sunday.getDisplayName(TextStyle.NARROW, locale)
                weekDayTwo.text = monday.getDisplayName(TextStyle.NARROW, locale)
                weekDayThree.text = tuesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFour.text = thursday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFive.text = wednesday.getDisplayName(TextStyle.NARROW, locale)
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
    }

    // TODO Remember the accessibility!

    fun setSundayBegin(sundayFirst: Boolean) {
        this.sundayFirst = sundayFirst
        invalidate()
        requestLayout()
    }

    fun setMonthNumber(month: Int) {
        this.month = month
        invalidate()
        requestLayout()
    }
}