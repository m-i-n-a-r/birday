package com.minar.birday.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.databinding.ActivityCompactWidgetConfigurationBinding
import com.minar.birday.utilities.addInsetsByPadding
import com.minar.birday.widgets.CompactWidgetProvider
import androidx.core.content.edit


class CompactWidgetConfigurationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompactWidgetConfigurationBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Retrieve the shared preferences
        val theme = sharedPrefs.getString("theme_color", "system")
        val accent = sharedPrefs.getString("accent_color", "system")


        // Avoid crashes when the widget is used before opening the app for the very first time
        if (sharedPrefs.getBoolean("first", true)) {
            sharedPrefs.edit {
                // Set default accent based on the Android version
                when (Build.VERSION.SDK_INT) {
                    23, 24, 25, 26, 27, 28, 29 -> putString("accent_color", "blue")
                    31 -> putString("accent_color", "system")
                    else -> putString("accent_color", "monet")
                }
            }
        }

        // Set the base theme and the accent
        when (theme) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "dark", "black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Set an amoled theme or a normal theme depending on amoled mode
        if (theme == "black") {
            setTheme(R.style.AppTheme)
            when (accent) {
                "monet" -> setTheme(R.style.AppTheme_Monet_PerfectDark)
                "system" -> setTheme(R.style.AppTheme_System_PerfectDark)
                "brown" -> setTheme(R.style.AppTheme_Brown_PerfectDark)
                "blue" -> setTheme(R.style.AppTheme_Blue_PerfectDark)
                "green" -> setTheme(R.style.AppTheme_Green_PerfectDark)
                "orange" -> setTheme(R.style.AppTheme_Orange_PerfectDark)
                "yellow" -> setTheme(R.style.AppTheme_Yellow_PerfectDark)
                "teal" -> setTheme(R.style.AppTheme_Teal_PerfectDark)
                "violet" -> setTheme(R.style.AppTheme_Violet_PerfectDark)
                "pink" -> setTheme(R.style.AppTheme_Pink_PerfectDark)
                "lightBlue" -> setTheme(R.style.AppTheme_LightBlue_PerfectDark)
                "red" -> setTheme(R.style.AppTheme_Red_PerfectDark)
                "lime" -> setTheme(R.style.AppTheme_Lime_PerfectDark)
                "crimson" -> setTheme(R.style.AppTheme_Crimson_PerfectDark)
                else -> setTheme(R.style.AppTheme_PerfectDark)
            }
        } else
            when (accent) {
                "monet" -> setTheme(R.style.AppTheme_Monet)
                "system" -> setTheme(R.style.AppTheme_System)
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
                else -> setTheme(R.style.AppTheme) // Default (aqua)
            }

        // Initialize the binding
        binding = ActivityCompactWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        binding.container.addInsetsByPadding(top = true, bottom = true, left = true, right = true)

        // Find the widget id from the intent
        val startIntent = intent
        val widgetId = startIntent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }


        // Restore the saved opacity value (default 80%)
        val savedOpacity = sharedPrefs.getInt("widget_compact_opacity", 80)
        val slider = binding.configurationOpacitySlider
        val opacityValue = binding.configurationOpacityValue
        val previewBackground = binding.previewContent

        slider.value = savedOpacity.toFloat()
        opacityValue.text = "$savedOpacity%"

        // Restore the saved show photos value (default true)
        val showPhotos = binding.configurationShowPhotosSwitch
        val savedShowPhotos = !sharedPrefs.getBoolean("widget_compact_hide_images", false)
        showPhotos.isChecked = savedShowPhotos

        // Date position spinner
        val datePositionSpinner = binding.configurationDatePositionSpinner
        val datePositionOptions = arrayOf(
            getString(R.string.compact_widget_date_below),
            getString(R.string.compact_widget_date_above),
            getString(R.string.compact_widget_date_hidden),
        )
        val datePositionValues = arrayOf("below", "above", "hidden")
        datePositionSpinner.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, datePositionOptions
        )
        val savedDatePosition = sharedPrefs.getString("widget_compact_date_position", "below") ?: "below"
        datePositionSpinner.setSelection(datePositionValues.indexOf(savedDatePosition).coerceAtLeast(0))

        // Zodiac position spinner
        val zodiacPositionSpinner = binding.configurationZodiacPositionSpinner
        val zodiacPositionOptions = arrayOf(
            getString(R.string.compact_widget_zodiac_hidden),
            getString(R.string.compact_widget_zodiac_before_date),
            getString(R.string.compact_widget_zodiac_after_date),
        )
        val zodiacPositionValues = arrayOf("hidden", "before", "after")
        zodiacPositionSpinner.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, zodiacPositionOptions
        )
        val savedZodiacPosition = sharedPrefs.getString("widget_compact_zodiac_position", "hidden") ?: "hidden"
        zodiacPositionSpinner.setSelection(zodiacPositionValues.indexOf(savedZodiacPosition).coerceAtLeast(0))

        // Preview avatars
        val previewAvatars = listOf(
            binding.previewAvatar1,
            binding.previewAvatar2,
            binding.previewAvatar3,
        )
        val avatarVisibility = if (savedShowPhotos) android.view.View.VISIBLE else android.view.View.GONE
        previewAvatars.forEach { it.visibility = avatarVisibility }

        // Toggle preview avatars when switch changes
        showPhotos.setOnCheckedChangeListener { _, isChecked ->
            val visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            previewAvatars.forEach { it.visibility = visibility }
        }

        val previewContent = binding.previewContent

        // Preview date views
        val previewDatesAbove = listOf(
            binding.previewDateAbove1, binding.previewDateAbove2, binding.previewDateAbove3
        )
        val previewDatesBelow = listOf(
            binding.previewDateBelow1, binding.previewDateBelow2, binding.previewDateBelow3
        )

        // Initialize preview date position + zodiac row visibility
        val zodiacRow = binding.configurationZodiacRow
        updatePreviewDatePosition(previewDatesAbove, previewDatesBelow, savedDatePosition)
        zodiacRow.visibility = if (savedDatePosition == "hidden") android.view.View.GONE else android.view.View.VISIBLE

        // Update preview when date position changes
        datePositionSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                val position = datePositionValues[pos]
                updatePreviewDatePosition(previewDatesAbove, previewDatesBelow, position)
                zodiacRow.visibility = if (position == "hidden") android.view.View.GONE else android.view.View.VISIBLE
                // Reset zodiac to hidden when date is hidden
                if (position == "hidden") zodiacPositionSpinner.setSelection(0)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Text size slider (default 12sp)
        val textSizeSlider = binding.configurationTextSizeSlider
        val textSizeValue = binding.configurationTextSizeValue
        val savedTextSize = sharedPrefs.getInt("widget_compact_text_size", 12)

        textSizeSlider.value = savedTextSize.toFloat()
        textSizeValue.text = "${savedTextSize} sp"
        updatePreviewTextSize(previewContent, savedTextSize.toFloat())

        textSizeSlider.addOnChangeListener { _, value, _ ->
            val size = value.toInt()
            textSizeValue.text = "${size} sp"
            updatePreviewTextSize(previewContent, size.toFloat())
        }

        // Highlight opacity slider (default 60%)
        val highlightOpacitySlider = binding.configurationHighlightOpacitySlider
        val highlightOpacityValue = binding.configurationHighlightOpacityValue
        val savedHighlightOpacity = sharedPrefs.getInt("widget_compact_highlight_opacity", 60)
        highlightOpacitySlider.value = savedHighlightOpacity.toFloat()
        highlightOpacityValue.text = "$savedHighlightOpacity%"

        // Unified color palette
        val allColors = linkedMapOf(
            "white" to Color.WHITE,
            "black" to Color.BLACK,
            "red" to getColor(R.color.red),
            "crimson" to getColor(R.color.crimson),
            "orange" to getColor(R.color.orange),
            "yellow" to getColor(R.color.yellow),
            "lime" to getColor(R.color.lime),
            "green" to getColor(R.color.green),
            "teal" to getColor(R.color.teal),
            "aqua" to getColor(R.color.aqua),
            "lightBlue" to getColor(R.color.lightBlue),
            "blue" to getColor(R.color.blue),
            "violet" to getColor(R.color.violet),
            "pink" to getColor(R.color.pink),
        )

        val previewHighlightRow = binding.previewHighlightRow

        // Current selections
        var selectedWidgetBgColor = sharedPrefs.getString("widget_compact_bg_color", "black") ?: "black"
        var selectedWidgetTextColor = sharedPrefs.getString("widget_compact_general_text_color", "white") ?: "white"
        var selectedHighlightBgColor = sharedPrefs.getString("widget_compact_highlight_color", "red") ?: "red"
        var selectedHighlightTextColor = sharedPrefs.getString("widget_compact_highlight_text_color", "white") ?: "white"

        // 1. Widget background color
        buildColorPicker(binding.bgColorPickerContainer, allColors, selectedWidgetBgColor) { name, color ->
            selectedWidgetBgColor = name
            updatePreviewBackground(previewBackground, slider.value.toInt(), color)
        }

        // 2. General text color
        buildColorPicker(binding.generalTextColorPickerContainer, allColors, selectedWidgetTextColor) { name, color ->
            selectedWidgetTextColor = name
            updatePreviewGeneralTextColor(previewContent, color)
        }

        // 3. Highlight background color
        buildColorPicker(binding.colorPickerContainer, allColors, selectedHighlightBgColor) { name, color ->
            selectedHighlightBgColor = name
            updatePreviewHighlight(previewHighlightRow, color, highlightOpacitySlider.value.toInt())
        }

        // 4. Highlight text color
        buildColorPicker(binding.textColorPickerContainer, allColors, selectedHighlightTextColor) { name, color ->
            selectedHighlightTextColor = name
            updatePreviewHighlightText(previewHighlightRow, color)
        }

        // Update preview highlight when opacity slider changes
        highlightOpacitySlider.addOnChangeListener { _, value, _ ->
            val hlOpacity = value.toInt()
            highlightOpacityValue.text = "$hlOpacity%"
            val color = allColors[selectedHighlightBgColor] ?: getColor(R.color.red)
            updatePreviewHighlight(previewHighlightRow, color, hlOpacity)
        }

        // Update preview bg when opacity slider changes (with selected color)
        slider.addOnChangeListener { _, value, _ ->
            val opacity = value.toInt()
            opacityValue.text = "$opacity%"
            val color = allColors[selectedWidgetBgColor] ?: Color.BLACK
            updatePreviewBackground(previewBackground, opacity, color)
        }

        // Initialize preview
        val initBgColor = allColors[selectedWidgetBgColor] ?: Color.BLACK
        val initTextColor = allColors[selectedWidgetTextColor] ?: Color.WHITE
        val initHlBgColor = allColors[selectedHighlightBgColor] ?: getColor(R.color.red)
        val initHlTextColor = allColors[selectedHighlightTextColor] ?: Color.WHITE
        updatePreviewBackground(previewBackground, savedOpacity, initBgColor)
        updatePreviewGeneralTextColor(previewContent, initTextColor)
        updatePreviewHighlight(previewHighlightRow, initHlBgColor, savedHighlightOpacity)
        updatePreviewHighlightText(previewHighlightRow, initHlTextColor)

        // Confirm button
        binding.configurationConfirmButton.setOnClickListener {
            val opacity = slider.value.toInt()
            sharedPrefs.edit {
                putInt("widget_compact_opacity", opacity)
                putBoolean("widget_compact_hide_images", !showPhotos.isChecked)
                putInt("widget_compact_text_size", textSizeSlider.value.toInt())
                putInt("widget_compact_highlight_opacity", highlightOpacitySlider.value.toInt())
                putString("widget_compact_bg_color", selectedWidgetBgColor)
                putString("widget_compact_general_text_color", selectedWidgetTextColor)
                putString("widget_compact_highlight_color", selectedHighlightBgColor)
                putString("widget_compact_highlight_text_color", selectedHighlightTextColor)
                putString("widget_compact_date_position", datePositionValues[datePositionSpinner.selectedItemPosition])
                putString("widget_compact_zodiac_position", zodiacPositionValues[zodiacPositionSpinner.selectedItemPosition])
            }

            // Trigger widget update
            val updateIntent = Intent(this, CompactWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    intArrayOf(widgetId)
                )
            }
            sendBroadcast(updateIntent)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    private fun createColorCircle(
        color: Int, selected: Boolean, size: Int, margin: Int
    ): android.widget.FrameLayout {
        val frame = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                marginEnd = margin
            }
            alpha = if (selected) 1.0f else 0.4f
        }
        // Filled circle
        val fill = android.widget.ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setImageResource(R.drawable.ic_dot_black_24dp)
            setColorFilter(color)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        // Ring border
        val ring = android.widget.ImageView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setImageResource(R.drawable.ic_ring_24dp)
            val luminance = Color.luminance(color)
            setColorFilter(if (luminance > 0.5f) Color.DKGRAY else Color.LTGRAY)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        frame.addView(fill)
        frame.addView(ring)
        return frame
    }

    private fun updatePreviewHighlight(row: android.view.View, color: Int, opacityPercent: Int) {
        val alpha = (opacityPercent * 255 / 100)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        row.setBackgroundColor(Color.argb(alpha, r, g, b))
    }

    private fun updatePreviewHighlightText(row: android.view.View, color: Int) {
        // Only color the last TextView (countdown "Heute!") in the row
        if (row is android.view.ViewGroup) {
            val lastChild = row.getChildAt(row.childCount - 1)
            if (lastChild is android.widget.TextView) {
                lastChild.setTextColor(color)
            }
        }
    }

    private fun buildColorPicker(
        container: android.widget.LinearLayout,
        colors: Map<String, Int>,
        selectedName: String,
        onColorSelected: (String, Int) -> Unit
    ) {
        val circleSize = (36 * resources.displayMetrics.density).toInt()
        val circleMargin = (4 * resources.displayMetrics.density).toInt()
        val views = mutableMapOf<String, android.view.View>()
        for ((name, color) in colors) {
            val circle = createColorCircle(color, name == selectedName, circleSize, circleMargin)
            circle.setOnClickListener {
                views.values.forEach { v -> v.alpha = 0.4f }
                circle.alpha = 1.0f
                onColorSelected(name, color)
            }
            views[name] = circle
            container.addView(circle)
        }
    }

    private fun updatePreviewBackground(view: android.view.View, opacityPercent: Int, color: Int = Color.BLACK) {
        val alpha = (opacityPercent * 255 / 100)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        view.setBackgroundColor(Color.argb(alpha, r, g, b))
    }

    private fun updatePreviewGeneralTextColor(container: android.view.ViewGroup, color: Int) {
        // Update text color for non-highlight rows (rows 1 and 2, index-based)
        for (i in 1 until container.childCount) {
            val row = container.getChildAt(i)
            if (row is android.view.ViewGroup) {
                setTextColorRecursive(row, color)
            }
        }
    }

    private fun setTextColorRecursive(group: android.view.ViewGroup, color: Int) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is android.widget.TextView) {
                child.setTextColor(color)
            } else if (child is android.view.ViewGroup) {
                setTextColorRecursive(child, color)
            }
        }
    }

    private fun updatePreviewTextSize(container: android.view.ViewGroup, sp: Float) {
        val smallSp = (sp * 0.78f) // date text is smaller
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            if (row is android.view.ViewGroup) {
                updateTextSizeRecursive(row, sp, smallSp)
            }
        }
    }

    private fun updateTextSizeRecursive(group: android.view.ViewGroup, sp: Float, smallSp: Float) {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is android.widget.TextView) {
                // Date texts have smaller size (identified by their lighter color)
                if (child.currentTextColor == android.graphics.Color.parseColor("#B3FFFFFF")) {
                    child.textSize = smallSp
                } else {
                    child.textSize = sp
                }
            } else if (child is android.view.ViewGroup) {
                updateTextSizeRecursive(child, sp, smallSp)
            }
        }
    }

    private fun updatePreviewDatePosition(
        above: List<android.view.View>, below: List<android.view.View>, position: String
    ) {
        when (position) {
            "hidden" -> {
                above.forEach { it.visibility = android.view.View.GONE }
                below.forEach { it.visibility = android.view.View.GONE }
            }
            "above" -> {
                above.forEach { it.visibility = android.view.View.VISIBLE }
                below.forEach { it.visibility = android.view.View.GONE }
            }
            else -> { // "below"
                above.forEach { it.visibility = android.view.View.GONE }
                below.forEach { it.visibility = android.view.View.VISIBLE }
            }
        }
    }
}
