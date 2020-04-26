package com.minar.birday

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.datetime.datePicker
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.persistence.Event
import com.minar.birday.utilities.AppRater
import com.minar.birday.viewmodels.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var adapter: EventAdapter

    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        adapter = EventAdapter(this.applicationContext, null)

        // Create the notification channel
        createNotificationChannel()

        // getSharedPreferences(MyPrefs, Context.MODE_PRIVATE); retrieves a specific shared preferences file
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = sp.getString("theme_color", "system")
        val accent = sp.getString("accent_color", "brown")

        // Set the base theme and the accent
        when (theme) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        when (accent) {
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
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the bottom navigation bar and configure it for the navigation plugin
        val navigation = findViewById<BottomNavigationView>(R.id.navigation)
        val navController: NavController = Navigation.findNavController(this, R.id.navHostFragment)
        navigation.setupWithNavController(navController)

        // Rating stuff
        AppRater.appLaunched(this)

        // Manage the fab
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            // Show a bottom sheet containing the form to insert a new event
            var nameValue  = "error"
            var surnameValue = ""
            var eventDateValue: LocalDate = LocalDate.of(1970,1,1)
            val dialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(16.toFloat())
                title(R.string.new_event)
                icon(R.drawable.ic_party_24dp)
                message(R.string.new_event_description)
                customView(R.layout.dialog_insert_event, scrollable = true)
                positiveButton(R.string.insert_event) {
                    // Use the data to create a event object and insert it in the db
                    val tuple = Event(
                        id = 0, originalDate = eventDateValue, name = nameValue.smartCapitalize(),
                        surname = surnameValue.smartCapitalize()
                    )

                    val thread = Thread {
                        homeViewModel.insert(tuple)
                        adapter.notifyDataSetChanged()
                    }
                    thread.start()

                    dismiss()
                }
                negativeButton(R.string.cancel_event_insert) {
                    dismiss()
                }
            }

            // Setup listeners and checks on the fields
            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
            val customView = dialog.getCustomView()
            val name = customView.findViewById<TextView>(R.id.nameEvent)
            val surname = customView.findViewById<TextView>(R.id.surnameEvent)
            val eventDate = customView.findViewById<TextView>(R.id.dateEvent)
            val endDate = Calendar.getInstance()
            var dateDialog: MaterialDialog? = null

            eventDate.setOnClickListener {
                // Prevent double dialogs on fast click
                if(dateDialog == null) {
                    dateDialog = MaterialDialog(this).show {
                        cancelable(false)
                        cancelOnTouchOutside(false)
                        datePicker(maxDate = endDate) { _, date ->
                            val year = date.get(Calendar.YEAR)
                            val month = date.get(Calendar.MONTH) + 1
                            val day = date.get(Calendar.DAY_OF_MONTH)
                            eventDateValue = LocalDate.of(year, month, day)
                            val formatter: DateTimeFormatter =
                                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                            eventDate.text = eventDateValue.format(formatter)
                        }
                    }
                    Handler().postDelayed({ dateDialog = null }, 750)
                }
            }

            // Validate each field in the form with the same watcher
            var nameCorrect = false
            var surnameCorrect = true // Surname is not mandatory
            var eventDateCorrect = false
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    when {
                        editable === name.editableText -> {
                            val nameText = name.text.toString()
                            if (nameText.isBlank() || !checkString(nameText)) {
                                name.error = getString(R.string.invalid_value_name)
                                dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                                nameCorrect = false
                            }
                            else {
                                nameValue = nameText
                                nameCorrect = true
                            }
                        }
                        editable === surname.editableText -> {
                            val surnameText = surname.text.toString()
                            if (!checkString(surnameText)) {
                                surname.error = getString(R.string.invalid_value_name)
                                dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                                surnameCorrect = false
                            }
                            else {
                                surnameValue = surnameText
                                surnameCorrect = true
                            }
                        }
                        editable === eventDate.editableText -> {
                            val eventDateText = eventDate.text.toString()
                            if (eventDateText.isBlank()) {
                                eventDate.error = getString(R.string.invalid_value_date)
                                dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                                eventDateCorrect = false
                            }
                            else {
                                eventDateCorrect = true
                            }
                        }
                    }
                    if(eventDateCorrect && nameCorrect && surnameCorrect) dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
                }
            }

            name.addTextChangedListener(watcher)
            surname.addTextChangedListener(watcher)
            eventDate.addTextChangedListener(watcher)
        }
    }

    // Create the NotificationChannel. When created the first time, this code does nothing
    private fun createNotificationChannel() {
        val name = getString(R.string.events_notification_channel)
        val descriptionText = getString(R.string.events_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("events_channel", name, importance).apply { description = descriptionText }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Some utility functions, used from every fragment connected to this activity

    // Vibrate using a standard vibration pattern
    fun vibrate() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val vib = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (sp.getBoolean("vibration", true)) // Vibrate if the vibration in options is set to on
            vib.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // Simply checks if the string is written using only letters
    fun checkString(submission : String): Boolean {
        for (s in submission.replace("\\s".toRegex(), "")) {
            if (s.isLetter()) continue
            else return false
        }
        return true
    }

    // Extension function to quickly capitalize a name, also considering other uppercase letter or multiple words
    @ExperimentalStdlibApi
    fun String.smartCapitalize(): String =
        trim().split(" ").joinToString(" ") { it.toLowerCase(Locale.ROOT).capitalize(Locale.ROOT) }
}
