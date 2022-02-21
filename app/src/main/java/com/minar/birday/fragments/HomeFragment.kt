package com.minar.birday.fragments

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.databinding.DialogAppsEventBinding
import com.minar.birday.databinding.FragmentHomeBinding
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.*
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidget
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit


@ExperimentalStdlibApi
class HomeFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()
    lateinit var adapter: EventAdapter
    lateinit var act: MainActivity
    lateinit var sharedPrefs: SharedPreferences
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var _dialogAppsEventBinding: DialogAppsEventBinding? = null
    private val dialogAppsEventBinding get() = _dialogAppsEventBinding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter(
            updateFavorite = { eventResult -> updateFavorite(eventResult) },
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
        val upcomingImage = binding.upcomingImage
        val shimmer = binding.homeCardShimmer
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val homeMotionLayout = binding.homeMain
        val homeCard = binding.homeCard
        val homeMiniFab = binding.homeMiniFab
        if (shimmerEnabled) shimmer.startShimmer()
        upcomingImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_party_popper)

        // Setup the search bar
        val searchBar = binding.homeSearch
        searchBar.addTextChangedListener { text ->
            mainViewModel.searchStringChanged(text.toString())
        }

        // Set motion layout state, since it's saved
        homeMotionLayout.progress = sharedPrefs.getFloat("home_motion_state", 0.0F)

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

        mainViewModel.allEvents.observe(viewLifecycleOwner) { events ->
            // Manage placeholders, search results and the main list
            events?.let { adapter.submitList(it) }
            if (events.isNotEmpty()) {
                insertUpcomingEvents(events)
                removePlaceholder()
            }
            if (events.isEmpty()) restorePlaceholders()
            if (events.isEmpty() && mainViewModel.searchString.value!!.isNotBlank())
                restorePlaceholders(true)
        }
        mainViewModel.nextEvents.observe(viewLifecycleOwner) { nextEvents ->
            // Update the widgets using the next events, to avoid strange behaviors when searching
            updateWidget(nextEvents)
        }

        // Restore search string in the search bar
        if (mainViewModel.searchString.value!!.isNotBlank())
            searchBar.setText(mainViewModel.searchString.value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset each binding to null to follow the best practice
        _binding = null
        _dialogAppsEventBinding = null
    }

    // Functions to update, delete and create an Event object to pass instead of the returning object passed
    private fun updateFavorite(eventResult: EventResult) {
        mainViewModel.update(resultToEvent(eventResult))
    }

    // Show a dialog with the details of the selected contact
    private fun onItemClick(position: Int) {
        // Return if there was a navigation, useful to avoid double tap on two events
        if (findNavController().currentDestination?.label != "fragment_home")
            return

        act.vibrate()
        val event = adapter.getItem(position)
        // Navigate to the new fragment passing in the event with safe args
        val action = HomeFragmentDirections.actionNavigationMainToDetailsFragment(event)
        findNavController().navigate(action)
    }

    // Show the next age and countdown on long press (only the latter for no year events)
    private fun onItemLongClick(position: Int) {
        act.vibrate()
        val event = adapter.getItem(position)
        val quickStat = if (event.yearMatter == false) formatDaysRemaining(
            getRemainingDays(event.nextDate!!),
            requireContext()
        )
        else "${getString(R.string.next_age)} ${getNextAge(event)}, " +
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
    private fun updateWidget(events: List<EventResult>) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val remoteViews = if (sharedPrefs.getBoolean("dark_widget", false)) RemoteViews(
            requireContext().packageName,
            R.layout.event_widget_dark
        )
        else RemoteViews(requireContext().packageName, R.layout.event_widget_light)
        val thisWidget = context?.let { ComponentName(it, EventWidget::class.java) }
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        // Make sure to show if there's more than one event
        val widgetUpcoming = when {
            // No events
            events.isEmpty() -> requireContext().getString(R.string.no_next_event)
            // Two events
            events.size == 2 && events[0].nextDate!!.isEqual(events[1].nextDate) ->
                events[0].name + " " + requireContext().getString(R.string.and) +
                        " " + events[1].name + ", " + nextDateFormatted(
                    events[0],
                    formatter,
                    requireContext()
                )
            events.size > 2 && events[0].nextDate!!.isEqual(events[1].nextDate) &&
                    !events[1].nextDate!!.isEqual(events[2].nextDate) ->
                events[0].name + " " + requireContext().getString(R.string.and) +
                        " " + events[1].name + ", " + nextDateFormatted(
                    events[0],
                    formatter,
                    requireContext()
                )
            // More than two events
            events.size > 2 && events[0].nextDate!!.isEqual(events[1].nextDate) &&
                    events[1].nextDate!!.isEqual(events[2].nextDate) ->
                events[0].name + " " + requireContext().getString(R.string.event_others) +
                        ", " + nextDateFormatted(events[0], formatter, requireContext())
            // One event
            else -> events[0].name + ", " + nextDateFormatted(
                events[0],
                formatter,
                requireContext()
            )
        }

        remoteViews.setOnClickPendingIntent(R.id.event_widget_main, pendingIntent)
        remoteViews.setTextViewText(R.id.event_widget_text, widgetUpcoming)
        appWidgetManager.updateAppWidget(thisWidget, remoteViews)
    }

    // Insert the necessary information in the upcoming event cardview (and confetti)
    private fun insertUpcomingEvents(events: List<EventResult>) {
        val cardTitle = binding.upcomingTitle
        val cardSubtitle = binding.upcomingSubtitle
        val cardDescription = binding.upcomingDescription
        var personName = ""
        var nextDateText = ""
        var nextAge = ""
        val upcomingDate = events[0].nextDate
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

        // Trigger confetti if there's an event today
        if (getRemainingDays(upcomingDate!!) == 0 && !mainViewModel.confettiDone) {
            triggerConfetti()
            mainViewModel.confettiDone = true
        }
        // Manage multiple events in the same day considering first case, middle cases and last case if more than 3
        for (event in events) {
            if (event.nextDate!!.isEqual(upcomingDate)) {
                // Consider the case of null surname and the case of unknown age
                val formattedPersonName =
                    formatName(event, sharedPrefs.getBoolean("surname_first", false))
                val age = if (event.yearMatter!!) event.nextDate.year.minus(event.originalDate.year)
                    .toString()
                else getString(R.string.unknown)
                when (events.indexOf(event)) {
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
        _dialogAppsEventBinding = DialogAppsEventBinding.inflate(LayoutInflater.from(context))
        val dialog =
            MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(res = R.dimen.rounded_corners)
                title(R.string.event_apps)
                icon(R.drawable.ic_apps_24dp)
                message(R.string.event_apps_description)
                customView(view = dialogAppsEventBinding.root, scrollable = true)
            }
        // Using view binding to fetch the buttons
        val whatsAppButton = dialogAppsEventBinding.whatsappButton
        val dialerButton = dialogAppsEventBinding.dialerButton
        val messagesButton = dialogAppsEventBinding.messagesButton
        val telegramButton = dialogAppsEventBinding.telegramButton
        val signalButton = dialogAppsEventBinding.signalButton
        val ctx: Context = requireContext()

        whatsAppButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("com.whatsapp")
            dialog.dismiss()
        }

        dialerButton.setOnClickListener {
            act.vibrate()
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                ctx.startActivity(dialIntent)
            } catch (e: Exception) {
                act.showSnackbar(ctx.getString(R.string.no_default_dialer))
            }
            dialog.dismiss()
        }

        messagesButton.setOnClickListener {
            act.vibrate()
            try {
                val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(requireContext())
                val smsIntent: Intent? =
                    ctx.packageManager.getLaunchIntentForPackage(defaultSmsPackage)
                ctx.startActivity(smsIntent)
            } catch (e: Exception) {
                act.showSnackbar(ctx.getString(R.string.no_default_sms))
            }
            dialog.dismiss()
        }

        telegramButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("org.telegram.messenger")
            dialog.dismiss()
        }

        signalButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("org.thoughtcrime.securesms")
            dialog.dismiss()
        }
    }

    private fun launchOrOpenAppStore(packageName: String) {
        try {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            requireContext().startActivity(intent)
        } catch (e: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                )
            )
        }
    }

    // Activate the confetti effect (stream, 3 colors, 4 shapes)
    private fun triggerConfetti() {
        val confetti = binding.confettiView
        confetti.build()
            .addColors(
                act.getThemeColor(android.R.attr.colorAccent),
                act.getThemeColor(android.R.attr.textColorPrimary),
                ContextCompat.getColor(requireContext(), R.color.goodGray),
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

