package com.minar.birday.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.DialogNotesBinding
import com.minar.birday.databinding.FragmentDetailsBinding
import com.minar.birday.fragments.dialogs.InsertEventBottomSheet
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.StatsGenerator
import com.minar.birday.utilities.addInsetsByPadding
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.formatDaysRemaining
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.getReducedDate
import com.minar.birday.utilities.getRemainingDays
import com.minar.birday.utilities.getStringForTypeCodename
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.utilities.resultToEvent
import com.minar.birday.viewmodels.MainViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class DetailsFragment : Fragment() {
    private lateinit var act: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var sharedPrefs: SharedPreferences
    private val args: DetailsFragmentArgs by navArgs()
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private var easterEggCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        act = activity as MainActivity

        // Recognize the image from the row of the recycler and animate the transition accordingly
        val animation = MaterialContainerTransform()
        animation.duration = 400
        animation.fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        animation.startElevation = 0f
        animation.endElevation = 0f
        animation.setAllContainerColors(getThemeColor(R.attr.backgroundColor, act))
        animation.scrimColor = getThemeColor(R.attr.backgroundColor, act)
        animation.isElevationShadowEnabled = false
        sharedElementEnterTransition = animation

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        val event = args.event
        val position = args.position

        val fullView = binding.detailsMotionLayout
        val shimmer = binding.detailsCountdownShimmer
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val astrologyDisabled = sharedPrefs.getBoolean("disable_astrology", false)
        val republicanCalendarDisabled = sharedPrefs.getBoolean("disable_republican_calendar", false)
        val hideImage = sharedPrefs.getBoolean("hide_images", false)
        val surnameFirst = sharedPrefs.getBoolean("surname_first", false)
        val titleText = formatName(event, surnameFirst)
        val title = binding.detailsEventName
        val image = binding.detailsEventImage
        val imageBg = binding.detailsEventImageBackground
        val deleteButton = binding.detailsDeleteButton
        val editButton = binding.detailsEditButton
        val shareButton = binding.detailsShareButton
        val notesButton = binding.detailsNotesButton

        // Manage the shimmer
        if (shimmerEnabled) {
            shimmer.startShimmer()
            shimmer.showShimmer(true)
        }

        // Add insets
        fullView.addInsetsByPadding(bottom = true)
        if (act.binding.bottomBar.hideOnScroll) {
            val navbarHeight = resources.getDimensionPixelSize(R.dimen.bottom_navbar_height)
            fullView.updatePadding(bottom = fullView.paddingBottom + navbarHeight)
        }

        // Bind the data on the views and set the transition name, to play it in reverse
        title.text = titleText
        if (hideImage) {
            image.visibility = View.GONE
            imageBg.visibility = View.GONE
            ViewCompat.setTransitionName(fullView, "shared_full_view$position")
        } else {
            ViewCompat.setTransitionName(image, "shared_image$position")
            ViewCompat.setTransitionName(title, "shared_title$position")
            if (event.image != null)
                image.setImageBitmap(byteArrayToBitmap(event.image))
            else {
                image.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        // Set the image depending on the event type
                        when (event.type) {
                            EventCode.BIRTHDAY.name -> R.drawable.placeholder_birthday_image
                            EventCode.ANNIVERSARY.name -> R.drawable.placeholder_anniversary_image
                            EventCode.DEATH.name -> R.drawable.placeholder_death_image
                            EventCode.NAME_DAY.name -> R.drawable.placeholder_name_day_image
                            else -> R.drawable.placeholder_other_image
                        }
                    )
                )
            }
            act.animateAvd(imageBg, R.drawable.animated_ripple_circle)
        }

        // Default animated vector drawable
        act.animateAvd(
            binding.detailsEventNameImage,
            R.drawable.animated_balloon,
            1500
        )

        // Small easter egg/motion on the image (with a slight zoom)
        image.setOnClickListener {
            easterEggCounter++
            if (easterEggCounter == 3) {
                easterEggCounter = 0
                if (binding.detailsMotionLayout.progress == 0F)
                    binding.detailsMotionLayout.transitionToEnd()
                else binding.detailsMotionLayout.transitionToStart()
            }
        }

        // Setup quick actions and corresponding navigation
        deleteButton.setOnClickListener {
            act.vibrate()
            deleteEvent(event)
            findNavController().popBackStack()
        }

        editButton.setOnClickListener {
            act.vibrate()
            editEvent(event)
        }

        shareButton.setOnClickListener {
            act.vibrate()
            shareEvent(event)
            findNavController().popBackStack()
        }

        // Manage the icon of the notes button (no notes / notes)
        if (event.notes.isNullOrBlank())
            (notesButton as MaterialButton).icon =
                AppCompatResources.getDrawable(act, R.drawable.ic_note_missing_24dp)

        notesButton.setOnClickListener {
            act.vibrate()
            val dialogNotesBinding = DialogNotesBinding.inflate(LayoutInflater.from(context))
            val notesTitle = "${getString(R.string.notes)} - ${event.name}"
            val noteTextField = dialogNotesBinding.favoritesNotes
            noteTextField.setText(event.notes)

            // Native dialog
            MaterialAlertDialogBuilder(act)
                .setView(dialogNotesBinding.root)
                .setTitle(notesTitle)
                .setIcon(R.drawable.ic_note_24dp)
                .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                    val note = noteTextField.text.toString().trim()
                    val tuple = Event(
                        id = event.id,
                        type = event.type,
                        originalDate = event.originalDate,
                        name = event.name,
                        yearMatter = event.yearMatter,
                        surname = event.surname,
                        favorite = event.favorite,
                        notes = note,
                        image = event.image
                    )
                    mainViewModel.update(tuple)
                    // Update locally (no livedata here)
                    event.notes = note
                    if (note.isBlank())
                        (notesButton as MaterialButton).icon =
                            AppCompatResources.getDrawable(act, R.drawable.ic_note_missing_24dp)
                    else
                        (notesButton as MaterialButton).icon =
                            AppCompatResources.getDrawable(act, R.drawable.ic_note_24dp)
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(android.R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        val formatter: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val subject: MutableList<EventResult> = mutableListOf()
        subject.add(event)
        val statsGenerator = StatsGenerator(subject, context)
        val daysRemaining = getRemainingDays(event.nextDate!!)
        val nextDateFormatted = event.nextDate.format(formatter)
        // Days remaining, plus next date properly formatted
        val daysCountdown =
            formatDaysRemaining(daysRemaining, requireContext()) + "\n" + nextDateFormatted
        binding.detailsZodiacSignValue.text =
            statsGenerator.getZodiacSign(event)
        binding.detailsCountdown.text = daysCountdown

        // Set republican calendar value
        if (republicanCalendarDisabled || !event.yearMatter!!) {
            binding.detailsRepublicanCalendar.visibility = View.GONE
            binding.detailsRepublicanCalendarValue.visibility = View.GONE
        } else {
            binding.detailsRepublicanCalendarValue.text = statsGenerator.getRepublicanDate(event)
        }

        // Manage the different event types
        if (event.type == (EventCode.BIRTHDAY.name)) {
            // Hide the age and the chinese sign and use a shorter birth date if the year is unknown
            if (!event.yearMatter!!) {
                binding.detailsNextAge.visibility = View.GONE
                binding.detailsNextAgeValue.visibility = View.GONE
                binding.detailsChineseSign.visibility = View.GONE
                binding.detailsChineseSignValue.visibility = View.GONE
                val reducedBirthDate = getReducedDate(event.originalDate)
                binding.detailsBirthDateValue.text = reducedBirthDate
            } else {
                binding.detailsNextAgeValue.text = getNextYears(event).toString()
                binding.detailsBirthDateValue.text =
                    event.originalDate.format(formatter)
                binding.detailsChineseSignValue.text =
                    statsGenerator.getChineseSign(event)
            }
            // Set the drawable of the zodiac sign or disable them entirely
            if (astrologyDisabled) disableAstrology()
            else when (statsGenerator.getZodiacSignNumber(event)) {
                0 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_sagittarius
                    )
                )

                1 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_capricorn
                    )
                )

                2 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_aquarius
                    )
                )

                3 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_pisces
                    )
                )

                4 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_aries
                    )
                )

                5 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_taurus
                    )
                )

                6 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_gemini
                    )
                )

                7 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_cancer
                    )
                )

                8 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_leo
                    )
                )

                9 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_virgo
                    )
                )

                10 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_libra
                    )
                )

                11 -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_zodiac_scorpio
                    )
                )
            }
        } else {
            // Not a birthday, set the drawable of the event type
            when (event.type) {
                EventCode.ANNIVERSARY.name -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_anniversary_24dp
                    )
                )

                EventCode.DEATH.name -> {
                    binding.detailsClearBackground.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(), R.drawable.ic_death_anniversary_24dp
                        )
                    )
                    act.animateAvd(
                        binding.detailsEventNameImage,
                        R.drawable.animated_candle_new,
                        1500
                    )
                }

                EventCode.NAME_DAY.name -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_name_day_24dp
                    )
                )

                EventCode.OTHER.name -> binding.detailsClearBackground.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(), R.drawable.ic_other_24dp
                    )
                )
            }
            // It makes no sense to write "age" when it's not a birthday, so just print the years
            if (event.yearMatter!!) {
                // Using another view instead of altering detailsNextAgeValue
                binding.detailsNextAgeValue.visibility = View.GONE
                binding.detailsNextAgeYears.visibility = View.VISIBLE
                binding.detailsNextAgeYears.text = String.format(
                    resources.getQuantityString(R.plurals.years, getNextYears(event)),
                    getNextYears(event)
                )
            } else
                binding.detailsNextAgeValue.visibility = View.GONE
            binding.detailsBirthDateValue.text =
                getStringForTypeCodename(requireContext(), event.type!!)
            binding.detailsBirthDate.visibility = View.GONE
            binding.detailsNextAge.visibility = View.GONE
            disableAstrology()
        }

        // Manage the predictive back between fragments
        val predictiveBackMargin = resources.getDimensionPixelSize(R.dimen.predictive_back_margin)
        var initialTouchY = -1f
        val background = binding.fragmentBackground
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                    val progress =
                        MainActivity.GestureInterpolator.getInterpolation(backEvent.progress)
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

        startPostponedEnterTransition()
    }

    // Delete an existing event and show a snackbar
    private fun deleteEvent(eventResult: EventResult) {
        mainViewModel.delete(resultToEvent(eventResult))
        act.showSnackbar(
            requireContext().getString(R.string.deleted),
            actionText = requireContext().getString(R.string.cancel),
            action = fun() = act.insertBack(eventResult),
        )
    }

    private fun editEvent(eventResult: EventResult) {
        val bottomSheet = InsertEventBottomSheet(act, eventResult)
        if (bottomSheet.isAdded) return
        bottomSheet.show(act.supportFragmentManager, "edit_event_bottom_sheet")
    }

    // Share an event as a plain string (plus some explanatory emotes) on every supported app
    private fun shareEvent(event: EventResult) {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        var typeEmoji = String(Character.toChars(0x1F973))
        when (event.type) {
            EventCode.ANNIVERSARY.name -> typeEmoji = String(Character.toChars(0x1F495))
            EventCode.DEATH.name -> typeEmoji = String(Character.toChars(0x1FAA6))
            EventCode.NAME_DAY.name -> typeEmoji = String(Character.toChars(0x1F607))
            EventCode.OTHER.name -> typeEmoji = String(Character.toChars(0x1F7E2))
        }
        val eventInformation =
            String(Character.toChars(0x1F388)) + "  " +
                    getString(R.string.notification_title) +
                    "\n" + typeEmoji + "  " +
                    formatName(event, sharedPrefs.getBoolean("surname_first", false)) +
                    " (" + getStringForTypeCodename(requireContext(), event.type!!) +
                    ")\n" + String(Character.toChars(0x1F56F)) + "  " +
                    event.nextDate!!.format(formatter) +
                    // Add a fourth line with the original date, if the year matters
                    if (event.yearMatter!!)
                        "\n" + String(Character.toChars(0x1F4C5)) + "  " +
                                event.originalDate.format(formatter)
                    else ""
        ShareCompat.IntentBuilder(requireActivity())
            .setText(eventInformation)
            .setType("text/plain")
            .setChooserTitle(getString(R.string.share_event))
            .startChooser()
    }

    // Disable any astrology related view
    private fun disableAstrology() {
        binding.detailsZodiacSign.visibility = View.GONE
        binding.detailsZodiacSignValue.visibility = View.GONE
        binding.detailsChineseSign.visibility = View.GONE
        binding.detailsChineseSignValue.visibility = View.GONE
    }
}