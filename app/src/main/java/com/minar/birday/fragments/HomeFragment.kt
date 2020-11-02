package com.minar.birday.fragments

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.datetime.datePicker
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.activities.SplashActivity
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.listeners.OnItemClickListener
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.*
import com.minar.birday.viewmodels.HomeViewModel
import com.minar.birday.widgets.EventWidget
import kotlinx.android.synthetic.main.dialog_apps_event.view.*
import kotlinx.android.synthetic.main.dialog_details_event.view.*
import kotlinx.android.synthetic.main.dialog_insert_event.view.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*


class HomeFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var homeViewModel: HomeViewModel
    lateinit var adapter: EventAdapter
    lateinit var act: MainActivity
    lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter(this)
        act = activity as MainActivity
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    @ExperimentalStdlibApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        val upcomingImage = v.findViewById<ImageView>(R.id.upcomingImage)
        val shimmer = v.findViewById<ShimmerFrameLayout>(R.id.homeCardShimmer)
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val homeMotionLayout = v.homeMain
        val homeCard = v.homeCard
        val homeMiniFab = v.homeMiniFab
        if (shimmerEnabled) shimmer.startShimmer()
        upcomingImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_party_popper)

        // Setup the search bar
        v.findViewById<EditText>(R.id.homeSearch).addTextChangedListener { text ->
            homeViewModel.searchNameChanged(text.toString())
        }

        // Set motion layout state, since it's saved
        homeMotionLayout.progress = sharedPrefs.getFloat("home_motion_state", 0.0F)

        // Vibration on the mini fab (with manual managing of the transition)
        homeMiniFab.setOnClickListener {
            when (homeMotionLayout.progress) {
                0.0F -> {
                    act.vibrate()
                    homeMotionLayout.transitionToEnd()
                    sharedPrefs.edit().putFloat("home_motion_state", 1.0F).apply()
                }
                1.0F -> {
                    act.vibrate()
                    homeMotionLayout.transitionToStart()
                    sharedPrefs.edit().putFloat("home_motion_state", 0.0F).apply()
                }
            }
        }

        // Open a micro app launcher
        homeCard.setOnClickListener {
            act.vibrate()
            val dialog =
                MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                    cornerRadius(res = R.dimen.rounded_corners)
                    title(R.string.event_apps)
                    icon(R.drawable.ic_apps_24dp)
                    message(R.string.event_apps_description)
                    customView(R.layout.dialog_apps_event, scrollable = true)
                }

            val customView = dialog.getCustomView()
            // Using viewbinding to fetch the buttons
            val whatsappButton = customView.whatsappButton
            val dialerButton = customView.dialerButton
            val messagesButton = customView.messagesButton
            val telegramButton = customView.telegramButton
            val ctx: Context = requireContext()

            whatsappButton.setOnClickListener {
                act.vibrate()
                try {
                    val whatsIntent: Intent? =
                        ctx.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ctx.startActivity(whatsIntent)
                } catch (e: Exception) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")
                        )
                    )
                }
                dialog.dismiss()
            }

            dialerButton.setOnClickListener {
                act.vibrate()
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL)
                    ctx.startActivity(dialIntent)
                } catch (e: Exception) {
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.no_default_dialer),
                        Toast.LENGTH_SHORT
                    ).show()
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
                    Toast.makeText(ctx, ctx.getString(R.string.no_default_sms), Toast.LENGTH_SHORT)
                        .show()
                }
                dialog.dismiss()
            }

            telegramButton.setOnClickListener {
                act.vibrate()
                try {
                    val telegramIntent: Intent? =
                        ctx.packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                    ctx.startActivity(telegramIntent)
                } catch (e: Exception) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger")
                        )
                    )
                }
                dialog.dismiss()
            }
        }
        rootView = v

        // Setup the recycler view
        initializeRecyclerView()
        setUpAdapter()

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.allEvents.observe(viewLifecycleOwner, { events ->
            // Manage placeholders, search results and the main list
            events?.let { adapter.submitList(it) }
            if (events.isNotEmpty()) {
                insertUpcomingEvents(events)
                removePlaceholder()
            }
            if (events.isEmpty()) restorePlaceholders()
            if (events.isEmpty() && homeViewModel.searchStringLiveData.value!!.isNotBlank())
                restorePlaceholders(true)
        })
        homeViewModel.nextEvents.observe(viewLifecycleOwner, { nextEvents ->
            // Update the widgets using the next events, to avoid strange behaviors when searching
            updateWidget(nextEvents)
        })

        return v
    }

    // Initialize the necessary parts of the recycler view
    private fun initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.eventRecycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    // Manage the onclick actions, or the long click (unused atm)
    @ExperimentalStdlibApi
    private fun setUpAdapter() {
        adapter.setOnItemClickListener(onItemClickListener = object : OnItemClickListener {
            // Show a dialog with the details of the selected contact
            override fun onItemClick(position: Int, view: View?) {
                act.vibrate()
                val event = adapter.getItem(position)
                val title = getString(R.string.event_details) + " - " + event.name
                val dialog = MaterialDialog(act).show {
                    title(text = title)
                    icon(R.drawable.ic_balloon_24dp)
                    cornerRadius(res = R.dimen.rounded_corners)
                    customView(R.layout.dialog_details_event, scrollable = true)
                    negativeButton(R.string.cancel) {
                        dismiss()
                    }
                }
                // Setup listeners and texts
                val customView = dialog.getCustomView()
                val deleteButton = customView.detailsDeleteButton
                val editButton = customView.detailsEditButton
                val shareButton = customView.detailsShareButton

                deleteButton.setOnClickListener {
                    act.vibrate()
                    deleteEvent(adapter.getItem(position))
                    dialog.dismiss()
                }

                editButton.setOnClickListener {
                    act.vibrate()
                    editEvent(adapter.getItem(position))
                    dialog.dismiss()
                }

                shareButton.setOnClickListener {
                    act.vibrate()
                    shareEvent(adapter.getItem(position))
                    dialog.dismiss()
                }

                val formatter: DateTimeFormatter =
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                val subject: MutableList<EventResult> = mutableListOf()
                subject.add(event)
                val statsGenerator = StatsGenerator(subject, context)
                val daysCountdown =
                    daysRemaining(getRemainingDays(event.nextDate!!), requireContext())
                customView.detailsZodiacSignValue.text = statsGenerator.getZodiacSign(event)
                customView.detailsCountdown.text = daysCountdown

                // Hide the age and the chinese sign and use a shorter birth date if the year is unknown
                if (!event.yearMatter!!) {
                    customView.detailsNextAge.visibility = View.GONE
                    customView.detailsNextAgeValue.visibility = View.GONE
                    customView.detailsChineseSign.visibility = View.GONE
                    customView.detailsChineseSignValue.visibility = View.GONE
                    val reducedBirthDate = getReducedDate(event.originalDate)
                    customView.detailsBirthDateValue.text = reducedBirthDate
                } else {
                    customView.detailsNextAgeValue.text = getNextAge(event).toString()
                    customView.detailsBirthDateValue.text = event.originalDate.format(formatter)
                    customView.detailsChineseSignValue.text = statsGenerator.getChineseSign(event)
                }
                // Set the drawable of the zodiac sign
                when (statsGenerator.getZodiacSignNumber(event)) {
                    0 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_sagittarius
                        )
                    )
                    1 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_capricorn
                        )
                    )
                    2 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_aquarius
                        )
                    )
                    3 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_pisces
                        )
                    )
                    4 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_aries
                        )
                    )
                    5 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_taurus
                        )
                    )
                    6 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_gemini
                        )
                    )
                    7 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_cancer
                        )
                    )
                    8 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_leo
                        )
                    )
                    9 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_virgo
                        )
                    )
                    10 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_libra
                        )
                    )
                    11 -> customView.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_scorpio
                        )
                    )
                }
            }

            // TODO reassign an action to the long press
            override fun onItemLongClick(position: Int, view: View?): Boolean {
                return true
            }
        })
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val placeholder: TextView = requireView().findViewById(R.id.noEvents) ?: return
        placeholder.visibility = View.GONE
    }

    // Restore the placeholder and texts when there are no events. If search is true, show the "no result" placeholder
    private fun restorePlaceholders(search: Boolean = false) {
        val cardTitle: TextView = requireView().findViewById(R.id.upcomingTitle)
        val cardSubtitle: TextView = requireView().findViewById(R.id.upcomingSubtitle)
        val cardDescription: TextView = requireView().findViewById(R.id.upcomingDescription)
        val placeholder: TextView = requireView().findViewById(R.id.noEvents)
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
        val intent = Intent(context, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
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

    // Insert the necessary information in the upcoming event cardview
    private fun insertUpcomingEvents(events: List<EventResult>) {
        val cardTitle: TextView = requireView().upcomingTitle
        val cardSubtitle: TextView = requireView().upcomingSubtitle
        val cardDescription: TextView = requireView().upcomingDescription
        var personName = ""
        var nextDateText = ""
        var nextAge = ""
        val upcomingDate = events[0].nextDate
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

        // Manage multiple events in the same day considering first case, middle cases and last case if more than 3
        for (event in events) {
            if (event.nextDate!!.isEqual(upcomingDate)) {
                // Consider the case of null surname and the case of unknown age
                val formattedPersonName =
                    formatName(event, sharedPrefs.getBoolean("surname_first", false))
                val age = if (event.yearMatter!!) event.nextDate.year.minus(event.originalDate.year)
                    .toString()
                else getString(R.string.unknown_age)
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

    // Functions to update, delete and create an Event object to pass instead of the returning object passed
    fun updateFavorite(eventResult: EventResult) = homeViewModel.update(toEvent(eventResult))

    fun deleteEvent(eventResult: EventResult) = homeViewModel.delete(toEvent(eventResult))

    private fun toEvent(eventResult: EventResult) = Event(
        id = eventResult.id,
        name = eventResult.name,
        surname = eventResult.surname,
        favorite = eventResult.favorite,
        originalDate = eventResult.originalDate,
        yearMatter = eventResult.yearMatter
    )

    @ExperimentalStdlibApi
    private fun editEvent(eventResult: EventResult) {
        var nameValue = eventResult.name
        var surnameValue = eventResult.surname
        var countYearValue = eventResult.yearMatter
        var eventDateValue: LocalDate = eventResult.originalDate
        val dialog = MaterialDialog(act, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            cornerRadius(res = R.dimen.rounded_corners)
            title(R.string.edit_event)
            icon(R.drawable.ic_edit_24dp)
            customView(R.layout.dialog_insert_event)
            positiveButton(R.string.update_event) {
                // Use the data to create a event object and update the db
                val tuple = Event(
                    id = eventResult.id,
                    originalDate = eventDateValue,
                    name = nameValue.smartCapitalize(),
                    yearMatter = countYearValue,
                    surname = surnameValue?.smartCapitalize(),
                    favorite = eventResult.favorite
                )
                homeViewModel.update(tuple)
                dismiss()
            }
            negativeButton(R.string.cancel) {
                dismiss()
            }
        }

        // Setup listeners and checks on the fields
        dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
        val customView = dialog.getCustomView()
        val name = customView.findViewById<TextView>(R.id.nameEvent)
        val surname = customView.findViewById<TextView>(R.id.surnameEvent)
        val eventDate = customView.findViewById<TextView>(R.id.dateEvent)
        val countYear = customView.findViewById<SwitchMaterial>(R.id.countYearSwitch)
        name.text = nameValue
        surname.text = surnameValue
        countYear.isChecked = countYearValue!!
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        eventDate.text = eventDateValue.format(formatter)
        val endDate = Calendar.getInstance()
        var dateDialog: MaterialDialog? = null

        // To automatically show the last selected date, parse it to another Calendar object
        val lastDate = Calendar.getInstance()
        lastDate.set(eventDateValue.year, eventDateValue.monthValue - 1, eventDateValue.dayOfMonth)

        // Update the boolean value on each click
        countYear.setOnCheckedChangeListener { _, isChecked ->
            countYearValue = isChecked
        }

        eventDate.setOnClickListener {
            // Prevent double dialogs on fast click
            if (dateDialog == null) {
                dateDialog = MaterialDialog(act).show {
                    cancelable(false)
                    cancelOnTouchOutside(false)
                    datePicker(maxDate = endDate, currentDate = lastDate) { _, date ->
                        val year = date.get(Calendar.YEAR)
                        val month = date.get(Calendar.MONTH) + 1
                        val day = date.get(Calendar.DAY_OF_MONTH)
                        eventDateValue = LocalDate.of(year, month, day)
                        eventDate.text = eventDateValue.format(formatter)
                        // If ok is pressed, the last selected date is saved if the dialog is reopened
                        lastDate.set(year, month - 1, day)
                    }
                }
                Handler(Looper.getMainLooper()).postDelayed({ dateDialog = null }, 750)
            }
        }

        // Validate each field in the form with the same watcher
        var nameCorrect = true
        var surnameCorrect = true
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                when {
                    editable === name.editableText -> {
                        val nameText = name.text.toString()
                        if (nameText.isBlank() || !checkString(nameText)) {
                            customView.nameEventLayout.error =
                                getString(R.string.invalid_value_name)
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                            nameCorrect = false
                        } else {
                            nameValue = nameText
                            customView.nameEventLayout.error = null
                            nameCorrect = true
                        }
                    }
                    editable === surname.editableText -> {
                        val surnameText = surname.text.toString()
                        if (!checkString(surnameText)) {
                            customView.surnameEventLayout.error =
                                getString(R.string.invalid_value_name)
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                            surnameCorrect = false
                        } else {
                            surnameValue = surnameText
                            customView.surnameEventLayout.error = null
                            surnameCorrect = true
                        }
                    }
                }
                if (nameCorrect && surnameCorrect) dialog.getActionButton(WhichButton.POSITIVE).isEnabled =
                    true
            }
        }

        name.addTextChangedListener(watcher)
        surname.addTextChangedListener(watcher)
        eventDate.addTextChangedListener(watcher)
    }

    // Share an event as a plain string (plus some explanatory emotes) on every supported app
    private fun shareEvent(event: EventResult) {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val eventInformation =
            String(Character.toChars(0x1F388)) + "  " +
                    getString(R.string.notification_title) +
                    "\n" + String(Character.toChars(0x1F973)) + "  " +
                    formatName(event, sharedPrefs.getBoolean("surname_first", false)) +
                    "\n" + String(Character.toChars(0x1F4C5)) + "  " +
                    event.nextDate!!.format(formatter)
        ShareCompat.IntentBuilder
            .from(requireActivity())
            .setText(eventInformation)
            .setType("text/plain")
            .setChooserTitle(getString(R.string.share_event))
            .startChooser()
    }

    // Loop the animated vector drawable
    internal fun ImageView.applyLoopingAnimatedVectorDrawable(@DrawableRes animatedVector: Int) {
        val animated = AnimatedVectorDrawableCompat.create(context, animatedVector)
        animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                this@applyLoopingAnimatedVectorDrawable.post { animated.start() }
            }
        })
        this.setImageDrawable(animated)
        animated?.start()
    }
}

