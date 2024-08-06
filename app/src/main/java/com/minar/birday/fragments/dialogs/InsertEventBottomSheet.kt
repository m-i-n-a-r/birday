package com.minar.birday.fragments.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
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
import com.minar.birday.adapters.ContactsFilterArrayAdapter
import com.minar.birday.databinding.BottomSheetInsertEventBinding
import com.minar.birday.model.ContactInfo
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.START_YEAR
import com.minar.birday.utilities.bitmapToByteArray
import com.minar.birday.utilities.checkName
import com.minar.birday.utilities.getAvailableTypes
import com.minar.birday.utilities.getBitmapSquareSize
import com.minar.birday.utilities.getStringForTypeCodename
import com.minar.birday.utilities.setEventImageOrPlaceholder
import com.minar.birday.utilities.smartFixName
import com.minar.birday.viewmodels.InsertEventViewModel
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.TimeZone


@OptIn(ExperimentalStdlibApi::class)
class InsertEventBottomSheet(
    private val act: MainActivity,
    private val event: EventResult? = null
) :
    BottomSheetDialogFragment() {
    private var _binding: BottomSheetInsertEventBinding? = null
    private val binding get() = _binding!!
    private lateinit var resultLauncher: ActivityResultLauncher<String>
    private var imageChosen = false
    private val viewModel: InsertEventViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the bottom sheet, initialize the shared preferences and the recent options list
        _binding = BottomSheetInsertEventBinding.inflate(inflater, container, false)

        // Result launcher stuff
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                // Handle the returned Uri (atm, the image can't be cropped)
                if (uri != null) {
                    imageChosen = true
                    setImage(uri)
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
        //vehicle insurance
        var manufacturerName = ""
        var manufacturerName1 = ""
        var manufacturerName2 = ""
        var manufacturerName3 = ""
        var modelName = ""
        var modelName1 = ""
        var modelName2 = ""
        var modelName3 = ""
        var insuranceProvider = ""
        // The initial date is today
        var eventDateValue: LocalDate = LocalDate.now()
        var dueDateValue: LocalDate = LocalDate.now()
        var countYearValue = true
        val positiveButton = binding.positiveButton
        val negativeButton = binding.negativeButton
        val eventImage = binding.imageEvent
        var typeValue = EventCode.BIRTHDAY.name
        positiveButton.isEnabled = false

        if (event != null) {
            typeValue = event.type!!
            nameValue = event.name
            surnameValue = event.surname ?: ""
            countYearValue = event.yearMatter ?: true
            eventDateValue = event.originalDate
            positiveButton.text = getString(R.string.update_event)
            title.text = getString(R.string.edit_event)
            //vehicle insurance
            manufacturerName = event.manufacturer_name ?: ""
            manufacturerName1 = event.manufacturer_name1 ?: ""
            manufacturerName2 = event.manufacturer_name2 ?: ""
            manufacturerName3 = event.manufacturer_name3 ?: ""

            modelName = event.model_name ?: ""
            modelName1 = event.model_name1 ?: ""
            modelName2 = event.model_name2 ?: ""
            modelName3 = event.model_name3 ?: ""

            insuranceProvider = event.insurance_provider ?: ""

            // Set the fields
            val type = binding.typeEvent
            val name = binding.nameEvent
            val surname = binding.surnameEvent
            val eventDate = binding.dateEvent
            val countYear = binding.countYearSwitch
            val vehicle_insurance_event_layout = binding.vehicleInsuranceLayout
            val other_event_layout = binding.otherEventLayout

            if(typeValue == "VEHICLE_INSURANCE"){
                other_event_layout.visibility=View.GONE
                vehicle_insurance_event_layout.visibility=View.VISIBLE
            }else{
                vehicle_insurance_event_layout.visibility=View.GONE
                other_event_layout.visibility=View.VISIBLE
            }

            type.setText(typeValue, false)
            name.setText(nameValue)
            surname.setText(surnameValue)
            countYear.isChecked = countYearValue
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            eventDate.setText(eventDateValue.format(formatter))
            imageChosen = setEventImageOrPlaceholder(event, eventImage)
            positiveButton.isEnabled = true

            //vehicle insurance
            binding.vehicleManufacturerEvent.setText(manufacturerName.toString())
            binding.vehicleManufacturerEvent1.setText(manufacturerName1.toString())
            binding.vehicleManufacturerEvent2.setText(manufacturerName2.toString())
            binding.vehicleManufacturerEvent3.setText(manufacturerName3.toString())

            binding.vehicleModelEvent.setText(modelName.toString())
            binding.vehicleModel1Event.setText(modelName1.toString())
            binding.vehicleModel2Event.setText(modelName2.toString())
            binding.vehicleModel3Event.setText(modelName3.toString())

            binding.vehicleInsuranceProviderEvent.setText(insuranceProvider.toString())

        }

        positiveButton.setOnClickListener {
            var image: ByteArray? = null
            if (imageChosen)
                image = bitmapToByteArray(eventImage.drawable.toBitmap())
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
                //vehicle insurance
                manufacturer_name = manufacturerName,
                manufacturer_name1 = manufacturerName1,
                manufacturer_name2 = manufacturerName2,
                manufacturer_name3 = manufacturerName3,

                model_name = modelName,
                model_name1 = modelName1,
                model_name2 = modelName2,
                model_name3 = modelName3,

                insurance_provider = insuranceProvider
            ) else
                Event(
                    id = 0,
                    originalDate = eventDateValue,
                    name = nameValue.smartFixName(),
                    surname = surnameValue.smartFixName(),
                    yearMatter = countYearValue,
                    type = typeValue,
                    image = image,
                    //vehicle insurance
                    manufacturer_name = manufacturerName,
                    manufacturer_name1 = manufacturerName1,
                    manufacturer_name2 = manufacturerName2,
                    manufacturer_name3 = manufacturerName3,

                    model_name = modelName,
                    model_name1 = modelName1,
                    model_name2 = modelName2,
                    model_name3 = modelName3,

                    insurance_provider = insuranceProvider
                )
            // Insert using another thread
            val thread = Thread {
                if (event != null) {
                    act.mainViewModel.update(tuple)
                    // Go back to the first screen to avoid updating the displayed details
                    act.runOnUiThread {
                        findNavController().popBackStack()
                        Toast.makeText(context, requireContext().getString(R.string.added_event), Toast.LENGTH_LONG).show()
                    }
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
        val countYear = binding.countYearSwitch

        binding.vehicleManufacturerEvent.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.vehicleManufacturerListLayout.visibility = View.VISIBLE
                binding.vehicleManufacturerEvent.requestFocus()

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.vehicleManufacturerEvent, InputMethodManager.SHOW_IMPLICIT)
            }
            true
        }

        binding.vehicleModelEvent.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.vehicleModelListLayout.visibility = View.VISIBLE
                binding.vehicleModelEvent.requestFocus()

                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.vehicleModelEvent, InputMethodManager.SHOW_IMPLICIT)
            }
            true
        }

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
                        countYear.isChecked = false
                        countYear.isEnabled = false
                        countYearValue = false

                        binding.otherEventLayout.visibility = View.VISIBLE
                        binding.vehicleInsuranceLayout.visibility = View.GONE
                    } else if(typeValue == EventCode.VEHICLE_INSURANCE.name){
                        binding.vehicleInsuranceLayout.visibility = View.VISIBLE
                        binding.otherEventLayout.visibility = View.GONE
                    }
                    else {
                        countYear.isChecked = true
                        countYear.isEnabled = true
                        countYearValue = true

                        binding.otherEventLayout.visibility = View.VISIBLE
                        binding.vehicleInsuranceLayout.visibility = View.GONE
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
                                    EventCode.VEHICLE_INSURANCE.name -> R.drawable.placeholder_vehicle_image
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

                        eventDate.setText(eventDateValue.format(formatter))
                        // The last selected date is saved if the dialog is reopened
                        lastDate.set(eventDateValue.year, month - 1, day)
                    }

                }
                // Show the picker and wait to reset the variable
                dateDialog!!.show(act.supportFragmentManager, "main_act_picker")
                Handler(Looper.getMainLooper()).postDelayed({ dateDialog = null }, 750)
            }
        }

        binding.dueDateEvent.setOnClickListener {
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
                        dueDateValue = LocalDate.of(year, month, day)

                        binding.dueDateEvent.setText(dueDateValue.format(formatter))
                        // The last selected date is saved if the dialog is reopened
                        lastDate.set(dueDateValue.year, month - 1, day)
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
        //vehicle insurance
        var manufacturerCorrect = false
        var modelCorrect = false
        var insuranceCorrect = false

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

                //vehicle insurance
                editable === binding.vehicleManufacturerEvent.editableText -> {
                    val manufacturer_name = binding.vehicleManufacturerEvent.text.toString()
                    if (manufacturer_name.isNotEmpty()) {
                        manufacturerName = manufacturer_name
                        binding.vehicleManufacturerLayout.error = null
                        manufacturerCorrect = true
                    } else {
                        // Setting the error on the layout is important to make the properties work
                        binding.vehicleManufacturerLayout.error =
                            getString(R.string.invalid_value_name)
                        positiveButton.isEnabled = false
                        manufacturerCorrect = false
                    }
                }

                editable === binding.vehicleManufacturerEvent1.editableText -> {
                    manufacturerName1 = binding.vehicleManufacturerEvent1.text.toString()
                }
                editable === binding.vehicleManufacturerEvent2.editableText -> {
                    manufacturerName2 = binding.vehicleManufacturerEvent2.text.toString()
                }
                editable === binding.vehicleManufacturerEvent3.editableText -> {
                    manufacturerName3 = binding.vehicleManufacturerEvent3.text.toString()
                }

                editable === binding.vehicleModel1Event.editableText -> {
                    modelName1 = binding.vehicleModel1Event.text.toString()
                }
                editable === binding.vehicleModel2Event.editableText -> {
                    modelName2 = binding.vehicleModel2Event.text.toString()
                }
                editable === binding.vehicleModel3Event.editableText -> {
                    modelName3 = binding.vehicleModel3Event.text.toString()
                }

                editable === binding.vehicleModelEvent.editableText -> {
                    val model_name = binding.vehicleModelEvent.text.toString()
                    if (model_name.isEmpty()) {
                        // Setting the error on the layout is important to make the properties work
                        binding.vehicleModelLayout.error =
                            getString(R.string.invalid_value_name)
                        positiveButton.isEnabled = false
                        modelCorrect = false
                    } else {
                        modelName = model_name
                        binding.vehicleManufacturerLayout.error = null
                        modelCorrect = true
                    }
                }

                editable === binding.vehicleInsuranceProviderEvent.editableText -> {
                    val insurance_provider_name = binding.vehicleInsuranceProviderEvent.text.toString()
                    if (insurance_provider_name.isEmpty()) {
                        // Setting the error on the layout is important to make the properties work
                        binding.insuranceProviderLayout.error =
                            getString(R.string.invalid_value_name)
                        positiveButton.isEnabled = false
                        insuranceCorrect = false
                    } else {
                        insuranceProvider = insurance_provider_name
                        binding.insuranceProviderLayout.error = null
                        insuranceCorrect = true
                    }
                }

            }

            if(typeValue == "VEHICLE_INSURANCE"){
               if(binding.dueDateEvent.text!!.isNotEmpty()){
                    eventDateCorrect = true
                }
                if (manufacturerCorrect && modelCorrect && insuranceCorrect && eventDateCorrect) positiveButton.isEnabled =true

            }else {
                if (eventDateCorrect && nameCorrect && surnameCorrect) positiveButton.isEnabled = true
            }
        }

        name.addTextChangedListener(watcher)
        surname.addTextChangedListener(watcher)
        eventDate.addTextChangedListener(watcher)
        //vehicle insurance
        binding.dueDateEvent.addTextChangedListener(watcher)
        binding.vehicleManufacturerEvent1.addTextChangedListener(watcher)
        binding.vehicleManufacturerEvent.addTextChangedListener(watcher)
        binding.vehicleManufacturerEvent2.addTextChangedListener(watcher)
        binding.vehicleManufacturerEvent3.addTextChangedListener(watcher)

        binding.vehicleModelEvent.addTextChangedListener(watcher)
        binding.vehicleModel1Event.addTextChangedListener(watcher)
        binding.vehicleModel2Event.addTextChangedListener(watcher)
        binding.vehicleModel3Event.addTextChangedListener(watcher)
        binding.vehicleInsuranceProviderEvent.addTextChangedListener(watcher)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }

    // Set the chosen image in the circular image
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

        // Bitmap ready. Avoid images larger than 450*450
        var dimension: Int = getBitmapSquareSize(bitmap)
        if (dimension > 450) dimension = 450

        val resizedBitmap = ThumbnailUtils.extractThumbnail(
            bitmap,
            dimension,
            dimension,
            ThumbnailUtils.OPTIONS_RECYCLE_INPUT,
        )
        val image = binding.imageEvent
        image.setImageBitmap(resizedBitmap)
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