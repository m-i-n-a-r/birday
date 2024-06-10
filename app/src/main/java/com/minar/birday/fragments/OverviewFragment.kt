package com.minar.birday.fragments

import android.content.SharedPreferences
import android.graphics.drawable.Animatable2
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.FragmentOverviewBinding
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.formatEventList
import com.minar.birday.viewmodels.MainViewModel
import com.minar.tasticalendar.model.TastiCalendarEvent
import com.minar.tasticalendar.model.TcSundayHighlight
import java.time.LocalDate


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
            if (advancedView) getString(R.string.overview) else getString(R.string.overview) + " - $yearNumber"
        binding.overviewTitle.text = title
        act.animateAvd(
            binding.overviewTitleImage,
            R.drawable.animated_overview,
            2500L
        )

        // Manage the yearly view
        val tcYear = binding.overviewYearView
        // Surely not a good approach, but it does the job, more or less

        val tcEvents: List<TastiCalendarEvent> =
            events.map {
                TastiCalendarEvent(
                    it.originalDate,
                    formatEventList(listOf(it), surnameFirst = false, act, showSurnames = false)
                )
            }

        // Snackbar related settings
        tcYear.apply {
            setSnackBarsDuration(5000, false)
            setSnackBarsPrefix(R.plurals.event, plural = true, false)
            setSundayHighlight(TcSundayHighlight.BOLDCOLORED, false)
            setSnackBarBaseView(act.findViewById(R.id.bottomBar)) // TODO Use binding
        }

        // Manage the advanced views and buttons
        if (advancedView) {
            val advancedYearTitle = binding.overviewAdvancedYear
            val nextButton = binding.overviewAdvancedNext
            val prevButton = binding.overviewAdvancedPrevious
            val appearance = sharedPrefs.getInt("overview_scale", 0)
            advancedYearTitle.visibility = View.VISIBLE
            advancedYearTitle.text = yearNumber.toString()
            nextButton.visibility = View.VISIBLE
            prevButton.visibility = View.VISIBLE
            nextButton.contentDescription = (yearNumber + 1).toString()
            prevButton.contentDescription = (yearNumber - 1).toString()
            tcYear.setAppearance(appearance)
            advancedYearTitle.setOnClickListener {
                yearNumber = LocalDate.now().year
                act.vibrate()
                tcYear.renderYear(yearNumber, tcEvents)
                advancedYearTitle.text = yearNumber.toString()
            }
            advancedYearTitle.setOnLongClickListener {
                // Cycles between the appearances
                val updatedAppearance = tcYear.setAppearance(-1)
                sharedPrefs.edit().putInt("overview_scale", updatedAppearance).apply()
                true
            }
            nextButton.setOnClickListener {
                (nextButton.drawable as Animatable2).start()
                act.vibrate()
                // Small easter egg
                if (yearNumber == 3000) {
                    act.showSnackbar(getString(R.string.wtf))
                    return@setOnClickListener
                }
                yearNumber += 1
                advancedYearTitle.text = yearNumber.toString()
                tcYear.renderYear(yearNumber, tcEvents)
            }
            prevButton.setOnClickListener {
                (prevButton.drawable as Animatable2).start()
                act.vibrate()
                // Small easter egg
                if (yearNumber == 0) {
                    act.showSnackbar(getString(R.string.wtf))
                    return@setOnClickListener
                }
                yearNumber -= 1
                advancedYearTitle.text = yearNumber.toString()
                tcYear.renderYear(yearNumber, tcEvents)
            }
        }

        // Finally, render the selected year
        tcYear.renderYear(yearNumber, tcEvents)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Manage the predictive back between fragments
        val predictiveBackMargin = resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
        var initialTouchY = -1f
        val background = binding.fragmentBackground
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    val progress = MainActivity.GestureInterpolator.getInterpolation(backEvent.progress)
                    if (initialTouchY < 0f) {
                        initialTouchY = backEvent.touchY
                    }
                    val progressY = MainActivity.GestureInterpolator.getInterpolation(
                        (backEvent.touchY - initialTouchY) / background.height
                    )

                    // Shift horizontally
                    val maxTranslationX = (background.width / 20) - predictiveBackMargin
                    background.translationX = progress * maxTranslationX *
                            (if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1)

                    // Shift vertically
                    val maxTranslationY = (background.height / 20) - predictiveBackMargin
                    background.translationY = progressY * maxTranslationY

                    // Scale down from 100% to 90%
                    val scale = 1f - (0.1f * progress)
                    background.scaleX = scale
                    background.scaleY = scale
                }

                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }

                override fun handleOnBackCancelled() {
                    initialTouchY = -1f
                    background.run {
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