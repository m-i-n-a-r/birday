package com.minar.birday.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.FragmentOverviewBinding
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable
import com.minar.birday.viewmodels.MainViewModel
import java.time.LocalDate


@ExperimentalStdlibApi
class OverviewFragment : Fragment() {
    private lateinit var act: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var sharedPrefs: SharedPreferences
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var events: List<EventResult>
    private var yearNumber: Int = LocalDate.now().year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        act = activity as MainActivity
        events = mainViewModel.allEventsUnfiltered.value ?: emptyList()

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
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

        val advancedView = sharedPrefs.getBoolean("advanced_overview", false)

        // Add some bottom padding to avoid hidden content behind the bottom navigation bar
        val hideNavbar = sharedPrefs.getBoolean("hide_scroll", false)
        if (hideNavbar) {
            binding.overviewMain.setPadding(
                0,
                0,
                0,
                act.resources.getDimension(R.dimen.bottom_navbar_height).toInt()
            )
        }

        val title: String =
            if (advancedView) getString(R.string.overview) else getString(R.string.overview) + " - $yearNumber}"
        binding.overviewTitle.text = title
        binding.overviewTitleImage.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_overview,
            2500L
        )

        // Manage the yearly view
        val minarYear = binding.overviewYearView

        // Manage the advanced views and buttons
        if (advancedView) {
            val advancedYearTitle = binding.overviewAdvancedYear
            val nextButton = binding.overviewAdvancedNext
            val prevButton = binding.overviewAdvancedPrevious
            advancedYearTitle.visibility = View.VISIBLE
            advancedYearTitle.text = yearNumber.toString()
            nextButton.visibility = View.VISIBLE
            prevButton.visibility = View.VISIBLE
            nextButton.contentDescription = (yearNumber + 1).toString()
            prevButton.contentDescription = (yearNumber - 1).toString()
            minarYear.setAdvancedInfoEnabled(true)
            nextButton.setOnClickListener {
                yearNumber += 1
                advancedYearTitle.text = yearNumber.toString()
                act.vibrate()
                minarYear.renderYear(yearNumber, events)
            }
            prevButton.setOnClickListener {
                act.vibrate()
                yearNumber -= 1
                advancedYearTitle.text = yearNumber.toString()
                minarYear.renderYear(yearNumber, events)
            }
        }

        // Finally, render the selected year
        minarYear.renderYear(yearNumber, events)

        return binding.root
    }


}