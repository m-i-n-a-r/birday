package com.minar.birday.fragments

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateFormat
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
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
import com.minar.birday.persistence.ContactsRepository
import com.minar.birday.utilities.StatsGenerator
import com.minar.birday.utilities.CASCADE_TIGHT_STAGGER
import com.minar.birday.utilities.addNavbarClearance
import com.minar.birday.utilities.animateCascade
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.daysMilestonesEnabled
import com.minar.birday.utilities.formatDaysLived
import com.minar.birday.utilities.formatDaysRemaining
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.formatTextPreview
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.getDaysLived
import com.minar.birday.utilities.EventCalendar
import com.minar.birday.utilities.alternativeCalendar
import com.minar.birday.utilities.formatInCalendar
import com.minar.birday.utilities.getNextUnbirthday
import com.minar.birday.utilities.getReducedDate
import com.minar.birday.utilities.getRemainingDays
import com.minar.birday.utilities.getStringForTypeCodename
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.utilities.isDaysMilestone
import com.minar.birday.utilities.resultToEvent
import com.minar.birday.utilities.streamBirdayConfetti
import com.minar.birday.utilities.unbirthdaysEnabled
import com.minar.birday.viewmodels.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


// The morph from the row into this page, and how far into it the rest of the content starts
// arriving: late enough that the image leads, early enough that the two land together
private const val SHARED_ELEMENT_DURATION = 400L
private const val CONTENT_CASCADE_DELAY = 140L
private const val CONTACT_FADE_DURATION = 220L
// The info pills pop in one after the other, with a hint of a spring
private const val PILL_STAGGER = 45L
private const val PILL_DURATION = 380L
private const val PILL_START_SCALE = 0.85f
private const val PILL_OVERSHOOT = 1.6f

class DetailsFragment : Fragment() {
    private lateinit var act: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var sharedPrefs: SharedPreferences
    private val args: DetailsFragmentArgs by navArgs()
    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!
    private var easterEggCounter = 0
    private var foundContactId: Long? = null
    // When the content cascade started, so a view arriving late can still take its own turn
    private var cascadeStartedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        act = activity as MainActivity

        // Recognize the image from the row of the recycler and animate the transition accordingly.
        // Going forward there is no gesture to follow, so the richer container transform is free to
        // be as unseekable as it likes
        val animation = MaterialContainerTransform()
        animation.duration = SHARED_ELEMENT_DURATION
        animation.fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        animation.startElevation = 0f
        animation.endElevation = 0f
        animation.setAllContainerColors(getThemeColor(android.R.attr.colorBackground, act))
        // The scrim is drawn in the overlay, above every view of the page. An opaque one hid the
        // content cascade for the whole morph and then vanished with it, so the page popped in all
        // at once. The page already has its own background: nothing needs covering
        animation.scrimColor = Color.TRANSPARENT
        animation.isElevationShadowEnabled = false
        sharedElementEnterTransition = animation

        // Coming back is a gesture, and a gesture needs seeking. A shared element transition on the
        // way out would be handled by a TransitionEffect, and that effect declares itself seekable
        // only when *every* operation it covers carries a non null seekable Transition of its own,
        // which a plain fragment does not. Not seekable means a cancelled back ends the pop
        // animators on their last frame instead of reversing them, and the page is left invisible.
        // Explicitly clearing the return transition drops the effect altogether: back is animators
        // only, like every other destination, and it survives being cancelled
        sharedElementReturnTransition = null

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
        val contactButton = binding.detailsContactButton

        // Spawn a contact button if a contact with the same name is found in the contacts (asynchronously)
        // If the contact was already found (e.g. view recreated), show the button immediately
        if (foundContactId != null) {
            contactButton.visibility = View.VISIBLE
            contactButton.setOnClickListener {
                try {
                    val contactUri = ContentUris.withAppendedId(
                        ContactsContract.Contacts.CONTENT_URI,
                        foundContactId!!
                    )
                    val intent = Intent(Intent.ACTION_VIEW, contactUri)
                    startActivity(intent)
                } catch (_: Exception) {
                    // Ignore malformed ID
                }
            }
        } else {
            contactButton.visibility = View.INVISIBLE
            try {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Capture contentResolver on the main thread before spawning the background thread
                    val contentResolver = requireContext().contentResolver
                    val surname = event.surname ?: ""
                    Thread {
                        try {
                            val contactId = ContactsRepository()
                                .findContactIdByName(contentResolver, event.name, surname)
                            if (contactId != null) {
                                Log.d("contacts", "Matching contact found for ${event.name}")
                                foundContactId = contactId.toLong()
                                activity?.runOnUiThread {
                                    if (isAdded && _binding != null) {
                                        // The lookup lands whenever it lands: fade in, don't pop
                                        contactButton.alpha = 0f
                                        contactButton.visibility = View.VISIBLE
                                        contactButton.animate()
                                            .alpha(1f)
                                            .setStartDelay(contactCascadeDelay(contactButton))
                                            .setDuration(CONTACT_FADE_DURATION)
                                            .start()
                                        contactButton.setOnClickListener {
                                            try {
                                                val contactUri = ContentUris.withAppendedId(
                                                    ContactsContract.Contacts.CONTENT_URI,
                                                    contactId.toLong()
                                                )
                                                val intent = Intent(Intent.ACTION_VIEW, contactUri)
                                                startActivity(intent)
                                            } catch (_: Exception) {
                                                // Ignore malformed ID
                                            }
                                        }
                                    }
                                }
                            } else {
                                Log.d("contacts", "No matching contact for ${event.name}")
                                activity?.runOnUiThread {
                                    if (isAdded && _binding != null) {
                                        // Probably redundant
                                        contactButton.visibility = View.INVISIBLE
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }.start()
                } else {
                    contactButton.visibility = View.GONE
                }
            } catch (_: Exception) {
                contactButton.visibility = View.GONE
            }
        }


        // Manage the shimmer
        if (shimmerEnabled) {
            shimmer.startShimmer()
            shimmer.showShimmer(true)
        }

        // Add insets
        fullView.addNavbarClearance()

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
                        image = event.image,
                        calendar = event.calendar
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
        val statsGenerator = StatsGenerator(subject, requireActivity())
        val daysRemaining = getRemainingDays(event.nextDate!!)
        val nextDateFormatted = event.nextDate.format(formatter)
        // Days remaining, plus next date properly formatted
        val daysCountdown =
            formatDaysRemaining(daysRemaining, requireContext()) + "\n" + nextDateFormatted
        binding.detailsZodiacSignValue.text =
            statsGenerator.getZodiacSign(event)
        binding.detailsCountdown.text = daysCountdown

        // Manage the different event types
        if (event.type == (EventCode.BIRTHDAY.name)) {
            // Hide the age and the chinese sign and use a shorter birthdate if the year is unknown
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

        // Days lived, for a birthday with a known year, only if opted in. A round thousand is a
        // party like a birthday
        val daysLived = if (daysMilestonesEnabled(act)) getDaysLived(event) else null
        if (daysLived == null) {
            binding.detailsDaysLived.visibility = View.GONE
            binding.detailsDaysLivedValue.visibility = View.GONE
        } else {
            val daysLivedValue = binding.detailsDaysLivedValue
            daysLivedValue.text = formatDaysLived(daysLived)
            if (isDaysMilestone(event)) {
                daysLivedValue.setTextColor(getThemeColor(R.attr.colorPrimary, act))
                daysLivedValue.setTypeface(daysLivedValue.typeface, Typeface.BOLD)
                // Once the morph has landed: before that the view has no size to rain from
                binding.detailsConfettiView.postDelayed({
                    _binding?.detailsConfettiView?.streamBirdayConfetti(act)
                }, SHARED_ELEMENT_DURATION)
            }
        }

        // Next unbirthday (#29), same day of the month and of the week as the birth, if opted in
        val nextUnbirthday = if (unbirthdaysEnabled(act)) getNextUnbirthday(event) else null
        if (nextUnbirthday == null) {
            binding.detailsUnbirthday.visibility = View.GONE
            binding.detailsUnbirthdayValue.visibility = View.GONE
        } else {
            binding.detailsUnbirthdayValue.text =
                if (nextUnbirthday == LocalDate.now()) getString(R.string.today)
                // Short weekday and month: the weekday is the whole point, the rest must fit a pill
                else nextUnbirthday.format(
                    DateTimeFormatter.ofPattern(
                        DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEyMMMd")
                    )
                )
        }

        // The date in the event's own calendar, while the alternative calendars are on
        val eventCalendar =
            if (alternativeCalendar(act) != null && event.yearMatter == true)
                EventCalendar.fromKey(event.calendar)
            else null
        if (eventCalendar == null) {
            binding.detailsCalendar.visibility = View.GONE
            binding.detailsCalendarValue.visibility = View.GONE
        } else {
            binding.detailsCalendarValue.text = formatInCalendar(event.originalDate, eventCalendar)
            binding.detailsCalendar.text = getString(eventCalendar.title)
        }

        // A pill is there only if its value is. The labels are shared with rows that end with a
        // colon, which a pill has no use for
        val pills = listOf(
            binding.detailsNextAgePill to binding.detailsNextAgeValue,
            binding.detailsZodiacSignPill to binding.detailsZodiacSignValue,
            binding.detailsChineseSignPill to binding.detailsChineseSignValue,
            binding.detailsDaysLivedPill to binding.detailsDaysLivedValue,
            binding.detailsUnbirthdayPill to binding.detailsUnbirthdayValue,
            binding.detailsCalendarPill to binding.detailsCalendarValue,
        )
        pills.forEach { (pill, value) -> pill.isVisible = value.isVisible }
        binding.detailsInfoPills.isVisible = pills.any { (pill, _) -> pill.isVisible }
        listOf(
            binding.detailsNextAge,
            binding.detailsZodiacSign,
            binding.detailsChineseSign,
            binding.detailsDaysLived,
            binding.detailsUnbirthday,
        ).forEach { it.text = it.text.trimEnd(':', ' ', '\u00A0') }
        startPostponedEnterTransition()
        // The rest of the page is claimed at alpha zero right now and rides in while the morph is
        // still traveling, so the image leads and the page follows it home instead of waiting for
        // it. The scene owns the positions inside a MotionLayout, so this is a fade and nothing else
        cascadeStartedAt = SystemClock.uptimeMillis()
        cascadeCandidates()
            .filter { it.isVisible }
            .animateCascade(
                translate = false,
                startDelay = CONTENT_CASCADE_DELAY,
                stagger = CASCADE_TIGHT_STAGGER
            )
        // While their container fades in, the pills themselves grow into place
        binding.detailsInfoPills.children.filter { it.isVisible }.forEachIndexed { index, pill ->
            pill.scaleX = PILL_START_SCALE
            pill.scaleY = PILL_START_SCALE
            pill.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(CONTENT_CASCADE_DELAY + PILL_STAGGER * index)
                .setDuration(PILL_DURATION)
                .setInterpolator(OvershootInterpolator(PILL_OVERSHOOT))
                .start()
        }
    }

    // The children riding in with the cascade, in order: everything but the shared elements
    private fun cascadeCandidates() = binding.detailsMotionLayout.children
        .filter {
            it !is Guideline && it.id !in setOf(
                R.id.expanderView,
                R.id.detailsEventImage,
                R.id.detailsEventName,
                R.id.detailsEventImageBackground,
                R.id.detailsConfettiView,
            )
        }
        .toList()

    // The contact lookup lands whenever it lands. The button waits for the turn it would have had
    // in the cascade, as if it had been visible from the start, instead of popping in on its own
    private fun contactCascadeDelay(contactButton: View): Long {
        val slot = cascadeCandidates()
            .filter { it.isVisible || it == contactButton }
            .indexOf(contactButton)
            .coerceAtLeast(0)
        val turn = CONTENT_CASCADE_DELAY + CASCADE_TIGHT_STAGGER * slot
        return (turn - (SystemClock.uptimeMillis() - cascadeStartedAt)).coerceAtLeast(0L)
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
        val eventInformation = formatTextPreview(
            event,
            act,
            sharedPrefs.getBoolean("surname_first", false),
            multiline = true
        )
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