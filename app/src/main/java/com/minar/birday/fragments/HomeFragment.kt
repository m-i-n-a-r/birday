package com.minar.birday.fragments

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
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
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.activities.SplashActivity
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.databinding.DialogAppsEventBinding
import com.minar.birday.databinding.DialogDetailsEventBinding
import com.minar.birday.databinding.DialogInsertEventBinding
import com.minar.birday.databinding.FragmentHomeBinding
import com.minar.birday.listeners.OnItemClickListener
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.*
import com.minar.birday.viewmodels.MainViewModel
import com.minar.birday.widgets.EventWidget
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*


class HomeFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var mainViewModel: MainViewModel
    lateinit var adapter: EventAdapter
    lateinit var act: MainActivity
    lateinit var sharedPrefs: SharedPreferences
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var _dialogDetailsBinding: DialogDetailsEventBinding? = null
    private val dialogDetailsBinding get() = _dialogDetailsBinding!!
    private var _dialogInsertEventBinding: DialogInsertEventBinding? = null
    private val dialogInsertEventBinding get() = _dialogInsertEventBinding!!
    private var _dialogAppsEventBinding: DialogAppsEventBinding? = null
    private val dialogAppsEventBinding get() = _dialogAppsEventBinding!!
    private lateinit var resultLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter(this)
        act = activity as MainActivity
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // Initialize the result launcher to pick the image
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                // Handle the returned Uri
                setImage(uri)
            }
    }

    @ExperimentalStdlibApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val v = binding.root
        val upcomingImage = binding.upcomingImage
        val shimmer = binding.homeCardShimmer
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val homeMotionLayout = binding.homeMain
        val homeCard = binding.homeCard
        val homeMiniFab = binding.homeMiniFab
        if (shimmerEnabled) shimmer.startShimmer()
        upcomingImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_party_popper)

        // Setup the search bar
        binding.homeSearch.addTextChangedListener { text ->
            mainViewModel.searchNameChanged(text.toString())
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
        rootView = v

        // Setup the recycler view
        initializeRecyclerView()
        setUpAdapter()

        mainViewModel = ViewModelProvider(act).get(MainViewModel::class.java)
        mainViewModel.allEvents.observe(viewLifecycleOwner, { events ->
            // Manage placeholders, search results and the main list
            events?.let { adapter.submitList(it) }
            if (events.isNotEmpty()) {
                insertUpcomingEvents(events)
                removePlaceholder()
            }
            if (events.isEmpty()) restorePlaceholders()
            if (events.isEmpty() && mainViewModel.searchStringLiveData.value!!.isNotBlank())
                restorePlaceholders(true)
        })
        mainViewModel.nextEvents.observe(viewLifecycleOwner, { nextEvents ->
            // Update the widgets using the next events, to avoid strange behaviors when searching
            updateWidget(nextEvents)
        })

        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset each binding to null to follow the best practice
        _binding = null
        _dialogAppsEventBinding = null
        _dialogDetailsBinding = null
        _dialogInsertEventBinding = null
    }

    // Initialize the necessary parts of the recycler view
    private fun initializeRecyclerView() {
        recyclerView = binding.eventRecycler
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
                _dialogDetailsBinding =
                    DialogDetailsEventBinding.inflate(LayoutInflater.from(context))
                val event = adapter.getItem(position)
                val title = getString(R.string.event_details) + " - " + event.name
                val dialog = MaterialDialog(act).show {
                    title(text = title)
                    icon(R.drawable.ic_balloon_24dp)
                    cornerRadius(res = R.dimen.rounded_corners)
                    customView(view = dialogDetailsBinding.root, scrollable = true)
                    negativeButton(R.string.cancel) {
                        dismiss()
                    }
                }
                // Setup listeners and texts
                val deleteButton = dialogDetailsBinding.detailsDeleteButton
                val editButton = dialogDetailsBinding.detailsEditButton
                val shareButton = dialogDetailsBinding.detailsShareButton

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
                    formatDaysRemaining(getRemainingDays(event.nextDate!!), requireContext())
                dialogDetailsBinding.detailsZodiacSignValue.text =
                    statsGenerator.getZodiacSign(event)
                dialogDetailsBinding.detailsCountdown.text = daysCountdown

                // Hide the age and the chinese sign and use a shorter birth date if the year is unknown
                if (!event.yearMatter!!) {
                    dialogDetailsBinding.detailsNextAge.visibility = View.GONE
                    dialogDetailsBinding.detailsNextAgeValue.visibility = View.GONE
                    dialogDetailsBinding.detailsChineseSign.visibility = View.GONE
                    dialogDetailsBinding.detailsChineseSignValue.visibility = View.GONE
                    val reducedBirthDate = getReducedDate(event.originalDate)
                    dialogDetailsBinding.detailsBirthDateValue.text = reducedBirthDate
                } else {
                    dialogDetailsBinding.detailsNextAgeValue.text = getNextAge(event).toString()
                    dialogDetailsBinding.detailsBirthDateValue.text =
                        event.originalDate.format(formatter)
                    dialogDetailsBinding.detailsChineseSignValue.text =
                        statsGenerator.getChineseSign(event)
                }
                // Set the drawable of the zodiac sign
                when (statsGenerator.getZodiacSignNumber(event)) {
                    0 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_sagittarius
                        )
                    )
                    1 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_capricorn
                        )
                    )
                    2 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_aquarius
                        )
                    )
                    3 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_pisces
                        )
                    )
                    4 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_aries
                        )
                    )
                    5 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_taurus
                        )
                    )
                    6 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_gemini
                        )
                    )
                    7 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_cancer
                        )
                    )
                    8 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_leo
                        )
                    )
                    9 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_virgo
                        )
                    )
                    10 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_libra
                        )
                    )
                    11 -> dialogDetailsBinding.detailsZodiacImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_zodiac_scorpio
                        )
                    )
                }
            }

            // Show the next age and countdown on long press (only the latter for no year events)
            override fun onItemLongClick(position: Int, view: View?): Boolean {
                act.vibrate()
                val event = adapter.getItem(position)
                val quickStat = if (event.yearMatter == false) formatDaysRemaining(
                    getRemainingDays(event.nextDate!!),
                    requireContext()
                )
                else "${getString(R.string.next_age)} ${getNextAge(event)}, " +
                        formatDaysRemaining(getRemainingDays(event.nextDate!!), requireContext())
                act.showSnackbar(quickStat)
                return true
            }
        })
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
    fun updateFavorite(eventResult: EventResult) {
        mainViewModel.update(resultToEvent(eventResult))
    }

    fun deleteEvent(eventResult: EventResult) = mainViewModel.delete(resultToEvent(eventResult))

    @ExperimentalStdlibApi
    private fun editEvent(eventResult: EventResult) {
        _dialogInsertEventBinding = DialogInsertEventBinding.inflate(LayoutInflater.from(context))
        var nameValue = eventResult.name
        var surnameValue = eventResult.surname
        var countYearValue = eventResult.yearMatter
        var eventDateValue: LocalDate = eventResult.originalDate
        val imageValue: ByteArray? = eventResult.image
        val dialog = MaterialDialog(act, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            cornerRadius(res = R.dimen.rounded_corners)
            title(R.string.edit_event)
            icon(R.drawable.ic_edit_24dp)
            customView(view = dialogInsertEventBinding.root)
            positiveButton(R.string.update_event) {
                val image = if (dialogInsertEventBinding.imageEvent.drawable != null)
                    bitmapToByteArray(dialogInsertEventBinding.imageEvent.drawable.toBitmap())
                else eventResult.image
                // Use the data to create an event object and update the db
                val tuple = Event(
                    id = eventResult.id,
                    originalDate = eventDateValue,
                    name = nameValue.smartCapitalize(),
                    yearMatter = countYearValue,
                    surname = surnameValue?.smartCapitalize(),
                    favorite = eventResult.favorite,
                    notes = eventResult.notes,
                    image = image
                )
                mainViewModel.update(tuple)
                dismiss()
            }
            negativeButton(R.string.cancel) {
                dismiss()
            }
        }

        // Setup listeners and checks on the fields
        dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
        val name = dialogInsertEventBinding.nameEvent
        val surname = dialogInsertEventBinding.surnameEvent
        val eventDate = dialogInsertEventBinding.dateEvent
        val countYear = dialogInsertEventBinding.countYearSwitch
        val eventImage = dialogInsertEventBinding.imageEvent
        name.setText(nameValue)
        surname.setText(surnameValue)
        countYear.isChecked = countYearValue!!
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        eventDate.setText(eventDateValue.format(formatter))
        if (imageValue != null)
            eventImage.setImageBitmap(byteArrayToBitmap(imageValue))

        val endDate = Calendar.getInstance()
        val startDate = Calendar.getInstance()
        startDate.set(1500, 1, 1)
        var dateDialog: MaterialDatePicker<Long>? = null

        // To automatically show the last selected date, parse it to another Calendar object
        val lastDate = Calendar.getInstance()
        lastDate.set(eventDateValue.year, eventDateValue.monthValue - 1, eventDateValue.dayOfMonth)

        // Update the boolean value on each click
        countYear.setOnCheckedChangeListener { _, isChecked ->
            countYearValue = isChecked
        }

        eventImage.setOnClickListener {
            resultLauncher.launch("image/*")
        }

        eventDate.setOnClickListener {
            // Prevent double dialogs on fast click
            if (dateDialog == null) {
                // Build constraints
                val constraints =
                    CalendarConstraints.Builder()
                        .setStart(startDate.timeInMillis)
                        .setEnd(endDate.timeInMillis)
                        .setValidator(DateValidatorPointBackward.now())
                        .build()

                // Build the dialog itself
                dateDialog =
                    MaterialDatePicker.Builder.datePicker()
                        .setTitleText(R.string.insert_date_hint)
                        .setSelection(lastDate.timeInMillis)
                        .setCalendarConstraints(constraints)
                        .build()

                // The user pressed ok
                dateDialog!!.addOnPositiveButtonClickListener {
                    val selection = it
                    if (selection != null) {
                        val date = Calendar.getInstance()
                        date.timeInMillis = selection
                        val year = date.get(Calendar.YEAR)
                        val month = date.get(Calendar.MONTH) + 1
                        val day = date.get(Calendar.DAY_OF_MONTH)
                        eventDateValue = LocalDate.of(year, month, day)
                        eventDate.setText(eventDateValue.format(formatter))
                        // The last selected date is saved if the dialog is reopened
                        lastDate.set(year, month - 1, day)
                    }

                }
                // Show the picker and wait to reset the variable
                dateDialog!!.show(parentFragmentManager, "home_picker")
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
                            dialogInsertEventBinding.nameEventLayout.error =
                                getString(R.string.invalid_value_name)
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                            nameCorrect = false
                        } else {
                            nameValue = nameText
                            dialogInsertEventBinding.nameEventLayout.error = null
                            nameCorrect = true
                        }
                    }
                    editable === surname.editableText -> {
                        val surnameText = surname.text.toString()
                        if (!checkString(surnameText)) {
                            dialogInsertEventBinding.surnameEventLayout.error =
                                getString(R.string.invalid_value_name)
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                            surnameCorrect = false
                        } else {
                            surnameValue = surnameText
                            dialogInsertEventBinding.surnameEventLayout.error = null
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
        val ctx: Context = requireContext()

        whatsAppButton.setOnClickListener {
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

    // Set the chosen image in the circular image
    private fun setImage(data: Uri?) {
        if (data == null) return
        var bitmap: Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT < 29) {
                @Suppress("DEPRECATION")
                bitmap = MediaStore.Images.Media.getBitmap(act.contentResolver, data)
            } else {
                val source = ImageDecoder.createSource(act.contentResolver, data)
                bitmap = ImageDecoder.decodeBitmap(source)
            }
        } catch (e: IOException) {
        }
        if (bitmap == null) return

        // Bitmap ready. Avoid images larger than 1000*1000
        var dimension: Int = getBitmapSquareSize(bitmap)
        if (dimension > 1000) dimension = 1000
        val resizedBitmap = ThumbnailUtils.extractThumbnail(
            bitmap,
            dimension,
            dimension,
            ThumbnailUtils.OPTIONS_RECYCLE_INPUT,
        )
        val image = dialogInsertEventBinding.imageEvent
        image.setImageBitmap(resizedBitmap)
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
            .setSpeed(1f, 5f)
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
            .streamFor(300, 3000L)
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

