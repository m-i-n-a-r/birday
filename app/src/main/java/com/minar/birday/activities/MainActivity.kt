package com.minar.birday.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.media.AudioAttributes
import android.media.AudioAttributes.Builder
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.minar.birday.R
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.backup.BirdayImporter
import com.minar.birday.backup.ContactsImporter
import com.minar.birday.databinding.ActivityMainBinding
import com.minar.birday.databinding.DialogInsertEventBinding
import com.minar.birday.model.Event
import com.minar.birday.utilities.*
import com.minar.birday.viewmodels.MainViewModel
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: EventAdapter
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var binding: ActivityMainBinding
    private var _dialogInsertEventBinding: DialogInsertEventBinding? = null
    private val dialogInsertEventBinding get() = _dialogInsertEventBinding!!
    private lateinit var resultLauncher: ActivityResultLauncher<String>
    private var imageChosen = false

    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        adapter = EventAdapter(null)
        // Initialize the result launcher to pick the image
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                // Handle the returned Uri
                if (uri != null) {
                    imageChosen = true
                    setImage(uri)
                }
            }

        // Create the notification channel and check the permission (note: appIntro 6.0 is still buggy, better avoid to use it for asking permissions)
        askContactsPermission()
        createNotificationChannel()

        // Retrieve the shared preferences
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = sharedPrefs.getString("theme_color", "system")
        val accent = sharedPrefs.getString("accent_color", "aqua")

        // Show the introduction for the first launch
        if (sharedPrefs.getBoolean("first", true)) {
            val editor = sharedPrefs.edit()
            editor.putBoolean("first", false)
            editor.apply()
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set the base theme and the accent
        when (theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        when (accent) {
            "brown" -> setTheme(R.style.AppTheme_Brown)
            "blue" -> setTheme(R.style.AppTheme_Blue)
            "green" -> setTheme(R.style.AppTheme_Green)
            "orange" -> setTheme(R.style.AppTheme_Orange)
            "yellow" -> setTheme(R.style.AppTheme_Yellow)
            "teal" -> setTheme(R.style.AppTheme_Teal)
            "violet" -> setTheme(R.style.AppTheme_Violet)
            "pink" -> setTheme(R.style.AppTheme_Pink)
            "lightBlue" -> setTheme(R.style.AppTheme_LightBlue)
            "red" -> setTheme(R.style.AppTheme_Red)
            "lime" -> setTheme(R.style.AppTheme_Lime)
            "crimson" -> setTheme(R.style.AppTheme_Crimson)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Get the bottom navigation bar and configure it for the navigation plugin
        val navigation = binding.navigation
        val navController: NavController = Navigation.findNavController(
            this,
            R.id.navHostFragment
        )
        // Only way to use custom animations with the bottom navigation bar
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setEnterAnim(R.anim.nav_enter_anim)
            .setExitAnim(R.anim.nav_exit_anim)
            .setPopEnterAnim(R.anim.nav_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_pop_exit_anim)
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        navigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigationMain ->
                    navController.navigate(R.id.navigationMain, null, options)
                R.id.navigationFavorites ->
                    navController.navigate(R.id.navigationFavorites, null, options)
                R.id.navigationSettings ->
                    navController.navigate(R.id.navigationSettings, null, options)
            }
            true
        }
        navigation.setOnNavigationItemReselectedListener {
            // Only do something if there's something in the back stack (only in event details)
            if (navController.currentBackStackEntry != null) navController.popBackStack()
        }

        // Rating stuff
        AppRater.appLaunched(this)

        // Manage the fab
        val fab = binding.fab
        fab.setOnClickListener {
            vibrate()
            _dialogInsertEventBinding = DialogInsertEventBinding.inflate(layoutInflater)
            // Show a bottom sheet containing the form to insert a new event
            var nameValue = "error"
            var surnameValue = ""
            var eventDateValue: LocalDate = LocalDate.of(1970, 1, 1)
            var countYearValue = true
            val dialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(res = R.dimen.rounded_corners)
                title(R.string.new_event)
                icon(R.drawable.ic_party_24dp)
                message(R.string.new_event_description)
                // Don't use scrollable here, instead use a nestedScrollView in the layout
                customView(view = dialogInsertEventBinding.root)
                positiveButton(R.string.insert_event) {
                    var image: ByteArray? = null
                    if (imageChosen)
                        image =
                            bitmapToByteArray(dialogInsertEventBinding.imageEvent.drawable.toBitmap())
                    // Use the data to create an event object and insert it in the db
                    val tuple = Event(
                        id = 0,
                        originalDate = eventDateValue,
                        name = nameValue.smartCapitalize(),
                        surname = surnameValue.smartCapitalize(),
                        yearMatter = countYearValue,
                        image = image,
                    )
                    // Insert using another thread
                    val thread = Thread { mainViewModel.insert(tuple) }
                    thread.start()
                    dismiss()
                }

                negativeButton(R.string.cancel) {
                    dismiss()
                }
            }

            // Setup listeners and checks on the fields
            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
            val name = dialogInsertEventBinding.nameEvent
            val surname = dialogInsertEventBinding.surnameEvent
            val eventDate = dialogInsertEventBinding.dateEvent
            val countYear = dialogInsertEventBinding.countYearSwitch
            val eventImage = dialogInsertEventBinding.imageEvent

            val endDate = Calendar.getInstance()
            val startDate = Calendar.getInstance()
            startDate.set(1500, 1, 1)
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            var dateDialog: MaterialDatePicker<Long>? = null

            // To automatically show the last selected date, parse it to another Calendar object
            val lastDate = Calendar.getInstance()

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
                    dateDialog!!.show(supportFragmentManager, "main_act_picker")
                    Handler(Looper.getMainLooper()).postDelayed({ dateDialog = null }, 750)
                }
            }

            // Validate each field in the form with the same watcher
            var nameCorrect = false
            var surnameCorrect = true // Surname is not mandatory
            var eventDateCorrect = false
            val watcher = object : TextWatcher {
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
                    when {
                        editable === name.editableText -> {
                            val nameText = name.text.toString()
                            if (nameText.isBlank() || !checkString(nameText)) {
                                // Setting the error on the layout is important to make the properties work. Kotlin synthetics are being used here
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
                                // Setting the error on the layout is important to make the properties work. Kotlin synthetics are being used here
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
                        // Once selected, the date can't be blank anymore
                        editable === eventDate.editableText -> eventDateCorrect = true
                    }
                    if (eventDateCorrect && nameCorrect && surnameCorrect) dialog.getActionButton(
                        WhichButton.POSITIVE
                    ).isEnabled = true
                }
            }

            name.addTextChangedListener(watcher)
            surname.addTextChangedListener(watcher)
            eventDate.addTextChangedListener(watcher)
        }
    }

    // Set the chosen image in the circular image
    private fun setImage(data: Uri) {
        var bitmap: Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT < 29) {
                @Suppress("DEPRECATION")
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data)
            } else {
                val source = ImageDecoder.createSource(this.contentResolver, data)
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

    // Create the NotificationChannel. This code does nothing when it already exists
    private fun createNotificationChannel() {
        val soundUri =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.birday_notification)
        val attributes: AudioAttributes = Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        val name = getString(R.string.events_notification_channel)
        val descriptionText = getString(R.string.events_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("events_channel", name, importance).apply {
            description = descriptionText
        }
        // Additional tuning over sound, vibration and notification light
        channel.setSound(soundUri, attributes)
        channel.enableLights(true)
        channel.lightColor = Color.GREEN
        channel.enableVibration(true)
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Choose a backup registering a callback and following the latest guidelines
    val selectBackup =
        registerForActivityResult(ActivityResultContracts.GetContent()) { fileUri: Uri? ->
            try {
                val birdayImporter = BirdayImporter(this, null)
                if (fileUri != null) birdayImporter.importBirthdays(this, fileUri)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


    // Some utility functions, used from every fragment connected to this activity

    // Vibrate using a standard vibration pattern
    fun vibrate() {
        val vib = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (sharedPrefs.getBoolean(
                "vibration",
                true
            )
        ) // Vibrate if the vibration in options is set to on
            vib.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // Return the accent color to use it programmatically
    fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    // Show a snackbar containing a given text and an optional action, with a 5 seconds duration
    fun showSnackbar(
        content: String,
        attachView: View? = null,
        action: (() -> Unit)? = null,
        actionText: String? = null,
    ) {
        val snackbar = Snackbar.make(binding.root, content, 5000)
        snackbar.isGestureInsetBottomIgnored = true
        if (attachView != null)
            snackbar.anchorView = attachView
        else
            snackbar.anchorView = binding.bottomBar
        if (action != null) {
            snackbar.setAction(actionText) {
                action()
            }
        }
        snackbar.show()
    }

    // Ask contacts permission
    fun askContactsPermission(code: Int = 101): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                code
            )
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    // Manage user response to permission requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            // Contacts at startup, show a snackbar only for permission denied (don't ask again not selected)
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS))
                        showSnackbar(getString(R.string.missing_permission_contacts))
                }
            }
            // Contacts while trying to import from contacts
            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS))
                        showSnackbar(getString(R.string.missing_permission_contacts))
                    else showSnackbar(getString(R.string.missing_permission_contacts_forever))
                } else {
                    val contactImporter = ContactsImporter(this, null)
                    contactImporter.importContacts(this)
                }
            }
        }
    }
}
