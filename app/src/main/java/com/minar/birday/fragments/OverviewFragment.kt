package com.minar.birday.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.FragmentOverviewBinding
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.MinarMonth
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.viewmodels.MainViewModel
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*


@ExperimentalStdlibApi
class OverviewFragment : Fragment() {
    private lateinit var act: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var events: List<EventResult>
    private lateinit var year: List<MinarMonth>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        act = activity as MainActivity
        events = mainViewModel.allEventsUnfiltered.value ?: emptyList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)

        val title: String = getString(R.string.overview) + " - ${LocalDate.now().year}"
        binding.overviewTitle.text = title
        binding.overviewTitleImage.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_overview,
            2500L
        )

        // Take every and each month to a list representing the year
        val january = binding.overviewJan
        val february = binding.overviewFeb
        val march = binding.overviewMar
        val april = binding.overviewApr
        val may = binding.overviewMay
        val june = binding.overviewJun
        val july = binding.overviewJul
        val august = binding.overviewAug
        val september = binding.overviewSep
        val october = binding.overviewOct
        val november = binding.overviewNov
        val december = binding.overviewDec
        year = listOf(
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
            for (month in year) {
                month.setSundayFirst(true)
            }
        }

        // Highlight the current date
        highlightCurrentDate()

        // Highlight the dates
        for (event in events)
            highlightDate(
                event.nextDate,
                getThemeColor(R.attr.colorPrimary, act),
                AppCompatResources.getDrawable(act, R.drawable.minar_month_circle),
                makeBold = false,
                autoOpacity = true,
                autoTextColor = true
            )

        return binding.root
    }

    // Highlight a date in a year, delegating the highlight to the correct month
    private fun highlightDate(
        date: LocalDate?,
        color: Int,
        drawable: Drawable?,
        makeBold: Boolean = false,
        autoOpacity: Boolean = false,
        autoTextColor: Boolean = false,
        asForeground: Boolean = false
    ) {
        if (date == null) return
        year[date.month.value - 1].highlightDay(
            date.dayOfMonth,
            color,
            drawable,
            makeBold = makeBold,
            autoOpacity = autoOpacity,
            autoTextColor = autoTextColor,
            asForeground = asForeground,
        )
    }

    // Highlight the current date with a ring
    private fun highlightCurrentDate(drawable: Drawable? = null, color: Int? = null) {
        val date = LocalDate.now()
        val chosenColor = color ?: getThemeColor(R.attr.colorTertiary, act)
        val chosenDrawable =
            drawable ?: AppCompatResources.getDrawable(act, R.drawable.minar_month_ring)
        highlightDate(date, chosenColor, chosenDrawable, asForeground = true)
    }
}