package com.minar.birday.fragments.dialogs

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.utilities.CASCADE_SHEET_DELAY
import com.minar.birday.utilities.animateChildrenCascade
import com.minar.birday.adapters.ContactsFilterArrayAdapter
import com.minar.birday.databinding.BottomSheetInsertEventBinding
import com.minar.birday.model.ContactInfo
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.*
import com.minar.birday.viewmodels.InsertEventViewModel
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


@OptIn(ExperimentalStdlibApi::class)
class InsertEventBottomSheet(
    private val act: MainActivity,
    private val event: EventResult? = null
) :
    BottomSheetDialogFragment() {
    private var _binding: BottomSheetInsertEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cropImageLauncher: ActivityResultLauncher<Uri>
    private var imageChosen = false
    private val viewModel: InsertEventViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the bottom sheet, initialize the shared preferences and the recent options list
        _binding = BottomSheetInsertEventBinding.inflate(inflater, container, false)

        // Two-step flow: pick an image from the gallery, then hand it to our crop activity.
        // Keeping them split makes each contract trivial and respects the library's recommended
        // pattern of embedding CropImageView inside an app-owned activity.
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) cropImageLauncher.launch(uri)
            }
        cropImageLauncher = registerForActivityResult(ImageCropContract()) { croppedUri ->
            if (croppedUri != null) {
                imageChosen = true
                setImage(croppedUri)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Fully expand the dialog
        (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Animate the drawable in loop
        val titleIcon = binding.insertEventImage
        val title = binding.insertEventTitle
        if (event == null) act.animateAvd(
            titleIcon,
            R.drawable.animated_insert_event,
            2500L
        )
        else act.animateAvd(titleIcon, R.drawable.animated_edit_event, 2500L)

        // Show a bottom sheet containing the form to insert a new event
        imageChosen = false
        var nameValue = "error"
        var surnameValue = ""
        // The initial date is today
        var eventDateValue: LocalDate = LocalDate.now()
        var countYearValue = true
        val positiveButton = binding.positiveButton
        val negativeButton = binding.negativeButton
        val eventImage = binding.imageEvent
        var typeValue = EventCode.BIRTHDAY.name
        positiveButton.isEnabled = false

        // The calendar of the event: its own when it has one, else the one in the settings. With
        // none chosen the switch never shows and the event keeps what it had
        val calendarGroup = binding.calendarGroup
        val eventCalendar =
            EventCalendar.fromKey(event?.calendar) ?: alternativeCalendar(act)
        val calendarAvailable = alternativeCalendar(act) != null && eventCalendar != null
        // The user's choice, kept aside while "without year" forces the Gregorian calendar
        var alternativeChosen = event == null || event.calendar != null
        if (calendarAvailable) {
            calendarGroup.visibility = View.VISIBLE
            binding.calendarAlternative.text = getString(eventCalendar.title)
        }

        if (event != null) {
            typeValue = event.type!!
            nameValue = event.name
            surnameValue = event.surname ?: ""
            countYearValue = event.yearMatter ?: true
            eventDateValue = event.originalDate
            positiveButton.text = getString(R.string.update_event)
            title.text = getString(R.string.edit_event)

            // Set the fields
            val type = binding.typeEvent
            val name = binding.nameEvent
            val surname = binding.surnameEvent
            val eventDate = binding.dateEvent
            val countYear = binding.countYearGroup
            type.setText(typeValue, false)
            name.setText(nameValue)
            surname.setText(surnameValue)
            countYear.check(if (countYearValue) R.id.countYearOn else R.id.countYearOff)
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            // Hide the year in the field if it doesn't matter
            eventDate.setText(
                if (countYearValue) eventDateValue.format(formatter)
                else forceMonthDayFormat(eventDateValue)
            )
            imageChosen = setEventImageOrPlaceholder(event, eventImage)
            positiveButton.isEnabled = true
        }
        positiveButton.setOnClickListener {
            var image: ByteArray? = null
            if (imageChosen)
                image = bitmapToByteArray(eventImage.drawable.toBitmap())
            // Without the year there's no day to convert from
            val calendarValue =
                if (!calendarAvailable) event?.calendar
                else if (alternativeChosen && countYearValue) eventCalendar.key
                else null
            // Use the data to create an event object and insert it in the db
            val tuple = if (event != null) Event(
                id = event.id,
                type = typeValue,
                originalDate = eventDateValue,
                name = nameValue.smartFixName(),
                yearMatter = countYearValue,
                surname = surnameValue.smartFixName(),
                favorite = event.favorite,
                notes = event.notes,
                image = image,
                calendar = calendarValue
            ) else
                Event(
                    id = 0,
                    originalDate = eventDateValue,
                    name = nameValue.smartFixName(),
                    surname = surnameValue.smartFixName(),
                    yearMatter = countYearValue,
                    type = typeValue,
                    image = image,
                    calendar = calendarValue,
                )
            // Insert using another thread
            val thread = Thread {
                if (event != null) {
                    act.mainViewModel.update(tuple)
                    // Go back to the first screen to avoid updating the displayed details
                    act.runOnUiThread { findNavController().popBackStack() }
                } else act.mainViewModel.insert(tuple)
            }
            thread.start()
            dismiss()
        }
        negativeButton.setOnClickListener {
            dismiss()
        }

        // Setup listeners and checks on the fields
        val type = binding.typeEvent
        val name = binding.nameEvent
        val surname = binding.surnameEvent
        val eventDate = binding.dateEvent
        val countYear = binding.countYearGroup

        // Set the dropdown to show the available event types
        val items = getAvailableTypes(act)
        val eventTypeAdapter = ArrayAdapter(act, R.layout.event_type_list_item, items)
        with(type) {
            setAdapter(eventTypeAdapter)
            setText(getStringForTypeCodename(context, typeValue), false)
            onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    typeValue = items[position].codeName.name
                    // Automatically uncheck "the year matters" for name days
                    if (typeValue == EventCode.NAME_DAY.name) {
                        countYear.check(R.id.countYearOff)
                        binding.countYearOn.isEnabled = false
                        binding.countYearOff.isEnabled = false
                        countYearValue = false
                    } else {
                        countYear.check(R.id.countYearOn)
                        binding.countYearOn.isEnabled = true
                        binding.countYearOff.isEnabled = true
                        countYearValue = true
                    }
                    if (!imageChosen)
                        eventImage.setImageDrawable(
                            ContextCompat.getDrawable(
                                context,
                                // Set the image depending on the event type
                                when (typeValue) {
                                    EventCode.BIRTHDAY.name -> R.drawable.placeholder_birthday_image
                                    EventCode.ANNIVERSARY.name -> R.drawable.placeholder_anniversary_image
                                    EventCode.DEATH.name -> R.drawable.placeholder_death_image
                                    EventCode.NAME_DAY.name -> R.drawable.placeholder_name_day_image
                                    else -> R.drawable.placeholder_other_image
                                }
                            )
                        )
                }
        }

        // Initialize contacts list, using InsertEventViewModel
        viewModel.initContactsList(act)
        viewModel.contactsList.observe(viewLifecycleOwner) { contacts ->
            // Setup AutoCompleteEditText adapters
            binding.nameEvent.setAdapter(
                ContactsFilterArrayAdapter(
                    requireContext(),
                    contacts,
                    ContactInfo::name,
                )
            )
            binding.surnameEvent.setAdapter(
                ContactsFilterArrayAdapter(
                    requireContext(),
                    contacts,
                    ContactInfo::surname,
                )
            )

            val onAutocompleteClick = AdapterView.OnItemClickListener { parent, _, i, _ ->
                val clickedItem =
                    parent.getItemAtPosition(i) as? ContactInfo ?: return@OnItemClickListener
                binding.nameEvent.setText(clickedItem.name)
                binding.surnameEvent.setText(clickedItem.surname)
            }
            binding.nameEvent.onItemClickListener = onAutocompleteClick
            binding.surnameEvent.onItemClickListener = onAutocompleteClick
        }

        // Calendar setup. The end date is the last day in the following year (dumb users)
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance()
        endDate.set(Calendar.YEAR, endDate.get(Calendar.YEAR) + 1)
        endDate.set(Calendar.DAY_OF_YEAR, endDate.getActualMaximum(Calendar.DAY_OF_YEAR))
        startDate.set(START_YEAR, 1, 1)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        var dateDialog: MaterialDatePicker<Long>? = null

        // To automatically show the last selected date, parse it to another Calendar object
        val lastDate = Calendar.getInstance()
        lastDate.set(eventDateValue.year, eventDateValue.monthValue - 1, eventDateValue.dayOfMonth)

        // Without the year there's no day to convert from: Gregorian, and nothing else to pick
        fun updateCalendarGroup() {
            calendarGroup.check(
                if (countYearValue && alternativeChosen) R.id.calendarAlternative
                else R.id.calendarGregorian
            )
            binding.calendarGregorian.isEnabled = countYearValue
            binding.calendarAlternative.isEnabled = countYearValue
        }
        updateCalendarGroup()
        calendarGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            // Only a choice of the user, not the Gregorian forced by "without year"
            if (!isChecked || !countYearValue) return@addOnButtonCheckedListener
            alternativeChosen = checkedId == R.id.calendarAlternative
        }

        // Update the boolean value on each click
        countYear.addOnButtonCheckedListener { _, checkedId, isChecked ->
            // The group reports the button leaving the selection too, only the incoming one counts
            if (!isChecked) return@addOnButtonCheckedListener
            countYearValue = checkedId == R.id.countYearOn
            updateCalendarGroup()
            // Reformat the date field, if already filled, to show or hide the year
            if (!eventDate.text.isNullOrBlank())
                eventDate.setText(
                    if (countYearValue) eventDateValue.format(formatter)
                    else forceMonthDayFormat(eventDateValue)
                )
        }

        eventImage.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        eventDate.setOnClickListener {
            // Prevent double dialogs on fast click
            if (dateDialog == null) {
                // Build constraints
                val constraints =
                    CalendarConstraints.Builder()
                        .setStart(startDate.timeInMillis)
                        .setEnd(endDate.timeInMillis)
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
                        // Use a standard timezone to avoid wrong date on different time zones
                        date.timeZone = TimeZone.getTimeZone("UTC")
                        date.timeInMillis = selection
                        val year = date.get(Calendar.YEAR)
                        val month = date.get(Calendar.MONTH) + 1
                        val day = date.get(Calendar.DAY_OF_MONTH)
                        eventDateValue = LocalDate.of(year, month, day)
                        val todayDate = LocalDate.now()
                        // The current year means the real one is not known, and a year still to
                        // come cannot be one to count from
                        if (typeValue != EventCode.NAME_DAY.name) {
                            val yearKnown =
                                year != todayDate.year && !eventDateValue.isAfter(todayDate)
                            countYear.check(if (yearKnown) R.id.countYearOn else R.id.countYearOff)
                            countYearValue = yearKnown
                        }
                        eventDate.setText(
                            if (countYearValue) eventDateValue.format(formatter)
                            else forceMonthDayFormat(eventDateValue)
                        )
                        // The last selected date is saved if the dialog is reopened
                        lastDate.set(eventDateValue.year, month - 1, day)
                    }

                }
                // Show the picker and wait to reset the variable
                dateDialog!!.show(act.supportFragmentManager, "main_act_picker")
                Handler(Looper.getMainLooper()).postDelayed({ dateDialog = null }, 750)
            }
        }

        // Validate each field in the form with the same watcher
        var nameCorrect = false
        var surnameCorrect = true // Surname is not mandatory
        var eventDateCorrect = event != null
        val watcher = afterTextChangedWatcher { editable ->
            when {
                editable === name.editableText -> {
                    val nameText = name.text.toString()
                    if (nameText.isBlank() || !checkName(nameText)) {
                        // Setting the error on the layout is important to make the properties work
                        binding.nameEventLayout.error =
                            getString(R.string.invalid_value_name)
                        positiveButton.isEnabled = false
                        nameCorrect = false
                    } else {
                        nameValue = nameText
                        binding.nameEventLayout.error = null
                        nameCorrect = true
                    }
                }

                editable === surname.editableText -> {
                    val surnameText = surname.text.toString()
                    if (!checkName(surnameText)) {
                        // Setting the error on the layout is important to make the properties work
                        binding.surnameEventLayout.error =
                            getString(R.string.invalid_value_name)
                        positiveButton.isEnabled = false
                        surnameCorrect = false
                    } else {
                        surnameValue = surnameText
                        binding.surnameEventLayout.error = null
                        surnameCorrect = true
                    }
                }
                // Once selected, the date can't be blank anymore
                editable === eventDate.editableText -> eventDateCorrect = true
            }
            if (eventDateCorrect && nameCorrect && surnameCorrect) positiveButton.isEnabled =
                true
        }
        name.addTextChangedListener(watcher)
        surname.addTextChangedListener(watcher)
        eventDate.addTextChangedListener(watcher)

        binding.insertEventBottomSheet.animateChildrenCascade(startDelay = CASCADE_SHEET_DELAY)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }

    // Set the chosen image in the circular image (called after crop, the bitmap is already cropped)
    private fun setImage(data: Uri) {
        var bitmap: Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT < 29) {
                @Suppress("DEPRECATION")
                bitmap = MediaStore.Images.Media.getBitmap(act.contentResolver, data)
            } else {
                val source = ImageDecoder.createSource(act.contentResolver, data)
                bitmap = ImageDecoder.decodeBitmap(source)
            }
        } catch (_: IOException) {
        }
        if (bitmap == null) return
        binding.imageEvent.setImageBitmap(bitmap)
    }

    private inline fun afterTextChangedWatcher(crossinline afterTextChanged: (editable: Editable) -> Unit) =
        object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable) {
                afterTextChanged(editable)
            }
        }
}
