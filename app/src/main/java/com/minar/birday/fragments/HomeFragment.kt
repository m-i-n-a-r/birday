package com.minar.birday.fragments

import android.animation.ObjectAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OVER_SCROLL_ALWAYS
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.databinding.FragmentHomeBinding
import com.minar.birday.fragments.dialogs.QuickAppsBottomSheet
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventDataItem
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.*
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidgetProvider
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit


@ExperimentalStdlibApi
class HomeFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: EventAdapter
    lateinit var act: MainActivity
    lateinit var sharedPrefs: SharedPreferences
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter(
            updateFavorite = { eventResult -> updateFavorite(eventResult) },
            showFavoriteHint = { showFavoriteHint() },
            onItemClick = { position -> onItemClick(position) },
            onItemLongClick = { position -> onItemLongClick(position) }
        )
        act = activity as MainActivity
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        val upcomingImage = binding.upcomingImage
        val shimmer = binding.homeCardShimmer
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val homeMotionLayout = binding.homeMain
        val homeCard = binding.homeCard
        val homeMiniFab = binding.homeMiniFab
        val typeSelector = binding.homeTypeSelector
        val searchBar = binding.homeSearch
        val searchBarLayout = binding.homeSearchLayout
        if (shimmerEnabled) shimmer.startShimmer()

        // Setup the search bar
        typeSelector.scaleX = 0F
        val listener = OnClickListener {
            if (searchBar.text.isNullOrBlank()) {
                typeSelector.visibility = View.VISIBLE
                typeSelector.pivotX = searchBarLayout.measuredWidth.toFloat()
                ObjectAnimator.ofFloat(typeSelector, "scaleX", 1.0f).apply {
                    duration = 300
                    interpolator = LinearOutSlowInInterpolator()
                    start()
                }
            } else {
                searchBar.setText("")
            }
        }
        searchBarLayout.setEndIconOnClickListener(listener)
        searchBar.addTextChangedListener { text ->
            mainViewModel.searchStringChanged(text.toString())
            if (text.isNullOrBlank()) searchBarLayout.setEndIconDrawable(R.drawable.ic_arrow_left_24dp)
            else searchBarLayout.setEndIconDrawable(R.drawable.ic_clear_24dp)
        }

        // Update the widget unconditionally
        updateWidget()

        // Setup the toggle buttons
        typeSelector.addOnButtonCheckedListener { _, checkedId, isChecked ->
            when (checkedId) {
                R.id.homeTypeSelectorBirthday -> {
                    // Only display events of type birthday
                    if (isChecked) {
                        mainViewModel.eventTypeChanged(EventCode.BIRTHDAY.name)
                    }
                }
                R.id.homeTypeSelectorAnniversary -> {
                    // Only display events of type anniversary
                    if (isChecked) {
                        mainViewModel.eventTypeChanged(EventCode.ANNIVERSARY.name)
                    }
                }
                R.id.homeTypeSelectorDeathAnniversary -> {
                    // Only display events of type death anniversary
                    if (isChecked) {
                        mainViewModel.eventTypeChanged(EventCode.DEATH.name)
                    }
                }
                R.id.homeTypeSelectorNameDay -> {
                    // Only display events of type name day
                    if (isChecked) {
                        mainViewModel.eventTypeChanged(EventCode.NAME_DAY.name)
                    }
                }
                R.id.homeTypeSelectorOther -> {
                    // Only display events of type other
                    if (isChecked) {
                        mainViewModel.eventTypeChanged(EventCode.OTHER.name)
                    }
                }
                R.id.homeTypeSelectorClose -> {
                    typeSelector.pivotX = 0F
                    searchBarLayout.setEndIconOnClickListener { return@setEndIconOnClickListener }
                    ObjectAnimator.ofFloat(typeSelector, "scaleX", 0.0f).apply {
                        duration = 200
                        interpolator = LinearOutSlowInInterpolator()
                        start()
                    }.doOnEnd {
                        typeSelector.visibility = View.GONE
                        typeSelector.clearChecked()
                        mainViewModel.eventTypeChanged("")
                        searchBarLayout.setEndIconOnClickListener(listener)
                    }
                }
            }
        }
        binding.homeTypeSelectorClose.setOnLongClickListener {
            typeSelector.clearChecked()
            true
        }

        // Set motion layout state, since it's saved
        homeMotionLayout.progress = sharedPrefs.getFloat("home_motion_state", 0.0F)

        // Set type selector visibility and selection
        if (!mainViewModel.selectedType.value.isNullOrBlank()) {
            typeSelector.scaleX = 1F
            typeSelector.visibility = View.VISIBLE
            when (mainViewModel.selectedType.value) {
                EventCode.BIRTHDAY.name -> typeSelector.check(R.id.homeTypeSelectorBirthday)
                EventCode.ANNIVERSARY.name -> typeSelector.check(R.id.homeTypeSelectorAnniversary)
                EventCode.DEATH.name -> typeSelector.check(R.id.homeTypeSelectorDeathAnniversary)
                EventCode.NAME_DAY.name -> typeSelector.check(R.id.homeTypeSelectorNameDay)
                EventCode.OTHER.name -> typeSelector.check(R.id.homeTypeSelectorOther)
            }
        }

        // Vibration on the mini fab (with manual managing of the transition)
        homeMiniFab.setOnClickListener {
            act.vibrate()
            when (homeMotionLayout.progress) {
                0.0F -> {
                    homeMotionLayout.transitionToEnd()
                    sharedPrefs.edit().putFloat("home_motion_state", 1.0F).apply()
                }
                1.0F -> {
                    homeMotionLayout.transitionToStart()
                    sharedPrefs.edit().putFloat("home_motion_state", 0.0F).apply()
                }
            }
        }

        // Activate the overscroll effect on Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.eventRecycler.overScrollMode = OVER_SCROLL_ALWAYS
        }

        // Show quick apps on long press too
        homeMiniFab.setOnLongClickListener {
            if (homeMotionLayout.progress == 1.0F) showQuickAppsSheet()
            true
        }

        // Open a micro app launcher
        homeCard.setOnClickListener {
            showQuickAppsSheet()
        }

        // Setup the recycler view
        binding.eventRecycler.adapter = adapter

        // The events, ordered and filtered by the eventual search
        mainViewModel.allEvents.observe(viewLifecycleOwner)
        { events ->
            // Manage placeholders, search results and the main list
            adapter.addHeadersAndSubmitList(events)

            if (events.isNotEmpty()) {
                // Insert the events in the upper card and remove the placeholders
                insertUpcomingEvents(events)
                removePlaceholder()
            } else {
                // Avd for empty card (same avd for no results or no events atm)
                upcomingImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_no_results)
                when {
                    mainViewModel.searchString.value!!.isNotBlank() -> restorePlaceholders(true)
                    mainViewModel.selectedType.value!!.isNotBlank() -> restorePlaceholders(true)
                    mainViewModel.searchString.value.isNullOrBlank() -> restorePlaceholders()
                    else -> removePlaceholder()
                }
            }
            (view.parent as? ViewGroup)?.doOnPreDraw { startPostponedEnterTransition() }
        }

        // Only the next events, without considering the search string, ordered
        mainViewModel.allEventsUnfiltered.observe(viewLifecycleOwner)
        {
            // Update the widgets using this livedata, to avoid strange behaviors when searching
            updateWidget()
        }

        // Restore search string in the search bar
        if (mainViewModel.searchString.value!!.isNotBlank())
            searchBar.setText(mainViewModel.searchString.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset each binding to null to follow the best practice
        _binding = null
    }

    // Functions to update, delete and create an Event object to pass instead of the returning object passed
    private fun updateFavorite(eventResult: EventResult) {
        mainViewModel.update(resultToEvent(eventResult))
    }

    // Show an hint when the star is long pressed
    private fun showFavoriteHint() {
        act.vibrate()
        act.showSnackbar(getString(R.string.add_favorite))
    }

    // Show a dialog with the details of the selected contact
    private fun onItemClick(position: Int) {
        // Return if there was a navigation, useful to avoid double tap on two events
        if (findNavController().currentDestination?.label != "fragment_home")
            return
        act.vibrate()
        // Cast required to obtain the original event result from the event item wrapper
        val event = (adapter.getItem(position) as EventDataItem.EventItem).eventResult
        val viewHolder: EventAdapter.EventViewHolder =
            binding.eventRecycler.findViewHolderForAdapterPosition(position) as EventAdapter.EventViewHolder
        // If the view is null or doesn't exist, nothing will happen
        val fullView = viewHolder.itemView
        val image = fullView.findViewById<ImageView>(R.id.eventImage)

        // Navigate to the new fragment passing in the event with safe args
        val action = HomeFragmentDirections.actionNavigationMainToDetailsFragment(event, position)

        // Play a different transition depending on the presence of the images
        val extras: FragmentNavigator.Extras = if (sharedPrefs.getBoolean("hide_images", false)) {
            FragmentNavigatorExtras(fullView to "shared_full_view$position")
        } else {
            FragmentNavigatorExtras(
                image to "shared_image$position",

                )
        }
        findNavController().navigate(action, extras)
    }

    // Show the next age and countdown on long press (only the latter for no year events)
    private fun onItemLongClick(position: Int) {
        act.vibrate()
        val event = (adapter.getItem(position) as EventDataItem.EventItem).eventResult
        val quickStat =
            if (event.yearMatter == false || event.type != EventCode.BIRTHDAY.name) formatDaysRemaining(
                getRemainingDays(event.nextDate!!),
                requireContext()
            )
            else "${getString(R.string.next_age)} ${getNextYears(event)}, " +
                    formatDaysRemaining(getRemainingDays(event.nextDate!!), requireContext())
        act.showSnackbar(quickStat)
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val placeholder = binding.noEvents
        placeholder.visibility = View.GONE
    }

    // Restore the placeholder and texts when there are no events. If search is true, show the "no result" placeholder
    private fun restorePlaceholders(search: Boolean = false) {
        val cardTitle: TextView = binding.upcomingTitle
        val cardSubtitle: TextView = binding.upcomingSubtitle
        val cardDescription: TextView = binding.upcomingDescription
        val placeholder: TextView = binding.noEvents
        if (!search) {
            cardTitle.text = getString(R.string.next_event)
            cardSubtitle.text = getString(R.string.no_next_event)
            cardDescription.text = getString(R.string.no_next_event_description)
        } else {
            cardTitle.text = getString(R.string.search_no_result_title)
            cardSubtitle.text = ""
            cardDescription.text = getString(R.string.search_no_result_description)
            placeholder.text = getString(R.string.search_no_result_title)
        }
        placeholder.visibility = View.VISIBLE
    }

    // Update the existing widgets with the newest data and the onclick action
    private fun updateWidget() {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.component = ComponentName(requireContext(), EventWidgetProvider::class.java)
        requireContext().sendBroadcast(intent)
    }

    // Insert the necessary information in the upcoming event card view (and confetti)
    private fun insertUpcomingEvents(events: List<EventResult>) {
        // First thing first, get the next events
        val nextEvents: List<EventResult> =
            if (events.indexOfFirst { it.nextDate != events[0].nextDate } == -1) events else
                events.subList(0, events.indexOfFirst { it.nextDate != events[0].nextDate })

        val cardTitle = binding.upcomingTitle
        val cardSubtitle = binding.upcomingSubtitle
        val cardDescription = binding.upcomingDescription
        val upcomingImage = binding.upcomingImage
        var personName = ""
        var nextDateText = ""
        var nextAge = ""
        val upcomingDate = nextEvents[0].nextDate
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

        // Set the correct avd
        when {
            nextEvents.all { it.type == EventCode.DEATH.name } -> upcomingImage.applyLoopingAnimatedVectorDrawable(
                R.drawable.animated_death_anniversary, 1000
            )
            nextEvents.all { it.type == EventCode.ANNIVERSARY.name } -> upcomingImage.applyLoopingAnimatedVectorDrawable(
                R.drawable.animated_anniversary, 1000
            )
            nextEvents.all { it.type == EventCode.NAME_DAY.name } -> upcomingImage.applyLoopingAnimatedVectorDrawable(
                R.drawable.animated_name_day, 1000
            )
            nextEvents.all { it.type == EventCode.OTHER.name } -> upcomingImage.applyLoopingAnimatedVectorDrawable(
                R.drawable.animated_other, 1000
            )
            else -> upcomingImage.applyLoopingAnimatedVectorDrawable(
                R.drawable.animated_party_popper,
                1000
            )
        }
        // Trigger confetti if there's an event today, except for "only death anniversaries" days
        if (
            getRemainingDays(upcomingDate!!) == 0 &&
            !mainViewModel.confettiDone &&
            !nextEvents.all { it.type == EventCode.DEATH.name }
        ) {
            triggerConfetti()
            mainViewModel.confettiDone = true
        }
        // Manage multiple events in the same day considering first case, middle cases and last case if more than 3
        for (event in nextEvents) {
            // Consider the case of null surname and the case of unknown age
            val formattedPersonName =
                formatName(event, sharedPrefs.getBoolean("surname_first", false))

            val age = if (event.yearMatter!! && event.type != EventCode.NAME_DAY.name)
                upcomingDate.year.minus(event.originalDate.year).toString()
            else if (event.type == EventCode.NAME_DAY.name) getString(R.string.name_day)
            else getString(R.string.unknown)
            when (nextEvents.indexOf(event)) {
                0 -> {
                    personName = formattedPersonName
                    nextDateText = nextDateFormatted(event, formatter, requireContext())
                    nextAge = getString(R.string.next_age_years) + ": $age"
                }
                1, 2 -> {
                    personName += ", $formattedPersonName"
                    nextAge += ", $age"
                }
                3 -> {
                    personName += " " + getString(R.string.event_others)
                    nextAge += "..."
                }
            }
            if (ChronoUnit.DAYS.between(event.nextDate, upcomingDate) < 0) break
        }
        cardTitle.text = personName
        cardSubtitle.text = nextDateText
        cardDescription.text = nextAge
    }

    // Show a bottom sheet containing some quick apps
    private fun showQuickAppsSheet() {
        act.vibrate()
        val bottomSheet = QuickAppsBottomSheet(act)
        if (bottomSheet.isAdded) return
        bottomSheet.show(act.supportFragmentManager, "quick_apps_bottom_sheet")
    }

    // Activate the confetti effect (stream, 3 colors, 4 shapes)
    private fun triggerConfetti() {
        val confetti = binding.confettiView
        confetti.build()
            .addColors(
                getThemeColor(R.attr.colorTertiary, act),
                getThemeColor(R.attr.colorSecondary, act),
                getThemeColor(R.attr.colorPrimary, act),
                getThemeColor(R.attr.colorOnSurface, act),
            )
            .setDirection(0.0, 359.0)
            .setSpeed(0.5f, 4f)
            .setRotationEnabled(true)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(
                Shape.DrawableShape(
                    ContextCompat.getDrawable(
                        act,
                        R.drawable.ic_triangle_24dp
                    )!!
                ),
                Shape.DrawableShape(
                    ContextCompat.getDrawable(
                        act,
                        R.drawable.ic_favorites_24dp
                    )!!
                ),
                Shape.DrawableShape(
                    ContextCompat.getDrawable(
                        act,
                        R.drawable.ic_star_24dp
                    )!!
                ),
                Shape.DrawableShape(
                    ContextCompat.getDrawable(
                        act,
                        R.drawable.ic_octagram_24dp
                    )!!
                )
            )
            .addSizes(Size(8), Size(12), Size(16))
            .setPosition(-50f, confetti.width + 50f, -50f, -50f)
            .streamFor(200, 2000L)
    }
}

