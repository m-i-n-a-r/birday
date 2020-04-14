package com.minar.birday

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import com.minar.birday.utils.AppRater
import java.time.LocalDateTime
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // getSharedPreferences(MyPrefs, Context.MODE_PRIVATE); retrieves a specific shared preferences file
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = sp.getString("theme_color", "system")
        val accent = sp.getString("accent_color", "brown")

        // Set the base theme and the accent
        if (theme == "system") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (theme == "dark") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        if (theme == "light") AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if (accent == "blue") setTheme(R.style.AppTheme_blue)
        if (accent == "green") setTheme(R.style.AppTheme_green)
        if (accent == "orange") setTheme(R.style.AppTheme_orange)
        if (accent == "yellow") setTheme(R.style.AppTheme_yellow)
        if (accent == "teal") setTheme(R.style.AppTheme_teal)
        if (accent == "violet") setTheme(R.style.AppTheme_violet)
        if (accent == "pink") setTheme(R.style.AppTheme_pink)
        if (accent == "lightBlue") setTheme(R.style.AppTheme_lightBlue)
        if (accent == "red") setTheme(R.style.AppTheme_red)
        if (accent == "lime") setTheme(R.style.AppTheme_lime)

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
        fab.setOnClickListener{
            // Show a bottom sheet containing the form to insert a new birthday
            val dialog = MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(16.toFloat())
                title(R.string.new_birthday)
                icon(R.drawable.ic_party_24dp)
                message(R.string.new_birthday_description)
                customView(R.layout.dialog_insert_birthday)
                positiveButton(R.string.insert_birthday) {
                    // TODO save the data in Room after controlling them
                    dismiss()
                }
                negativeButton(R.string.cancel_birthday) {
                    dismiss()
                }
            }

            // Setup listeners and checks on the fields
            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
            val customView = dialog.getCustomView()
            val name = customView.findViewById<TextView>(R.id.nameBirthday)
            val surname = customView.findViewById<TextView>(R.id.surnameBirthday)
            val birthDate = customView.findViewById<TextView>(R.id.dateBirthday)
            val endDate = Calendar.getInstance()

            birthDate.setOnClickListener {
                MaterialDialog(this).show {
                    datePicker(maxDate = endDate) { dialog, date ->
                        val year = date.get(Calendar.YEAR)
                        val month = date.get(Calendar.MONTH)
                        val day = date.get(Calendar.DAY_OF_MONTH)
                        val selectedDate = day.toString() + month.toString() + year.toString()
                        birthDate.text = selectedDate
                    }
                }
            }

            name.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    name.text.toString()
                }
            })

            surname.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    surname.text.toString()
                }
            })

            birthDate.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    birthDate.text.toString()
                }
            })

        }



    }

    // Some utility functions, used from every fragment connected to this activity
    fun vibrate() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val vib = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (sp.getBoolean("vibration", true)) // Vibrate if the vibration in options is set to on
            vib.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

}
