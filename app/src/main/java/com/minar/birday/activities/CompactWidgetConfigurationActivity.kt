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
import com.minar.birday.widgets.CompactWidgetRemoteViewsFactory
import androidx.core.content.edit


class CompactWidgetConfigurationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompactWidgetConfigurationBinding
    private lateinit var sharedPrefs: SharedPreferences

    private val datePositionValues = arrayOf("below", "above", "hidden")
    private val zodiacPositionValues = arrayOf("hidden", "before", "after")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        applyAppTheme()

        binding = ActivityCompactWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        binding.container.addInsetsByPadding(top = true, bottom = true, left = true, right = true)

        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setupOpacitySlider()
        setupPhotosSwitch()
        setupDatePositionSpinner()
        setupZodiacPositionSpinner()
        setupTextSizeSlider()
        setupHighlightOpacitySlider()
        setupColorPickers()
        initializePreview()
        setupConfirmButton(widgetId)
    }

    private fun applyAppTheme() {
        val theme = sharedPrefs.getString("theme_color", "system")
        val accent = sharedPrefs.getString("accent_color", "system")

        if (sharedPrefs.getBoolean("first", true)) {
            sharedPrefs.edit {
                when (Build.VERSION.SDK_INT) {
                    23, 24, 25, 26, 27, 28, 29 -> putString("accent_color", "blue")
                    31 -> putString("accent_color", "system")
                    else -> putString("accent_color", "monet")
                }
            }
        }

        when (theme) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "dark", "black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

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
                else -> setTheme(R.style.AppTheme)
            }
    }

    private fun setupOpacitySlider() {
        val savedOpacity = sharedPrefs.getInt("widget_compact_opacity", 80)
        binding.configurationOpacitySlider.value = savedOpacity.toFloat()
        binding.configurationOpacityValue.text = "$savedOpacity%"
    }

    private fun setupPhotosSwitch() {
        val savedShowPhotos = !sharedPrefs.getBoolean("widget_compact_hide_images", false)
        binding.configurationShowPhotosSwitch.isChecked = savedShowPhotos

        val previewAvatars = listOf(
            binding.previewAvatar1, binding.previewAvatar2, binding.previewAvatar3,
        )
        previewAvatars.forEach {
            it.visibility = if (savedShowPhotos) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.configurationShowPhotosSwitch.setOnCheckedChangeListener { _, isChecked ->
            val visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            previewAvatars.forEach { it.visibility = visibility }
        }
    }

    private fun setupDatePositionSpinner() {
        val datePositionOptions = arrayOf(
            getString(R.string.compact_widget_date_below),
            getString(R.string.compact_widget_date_above),
            getString(R.string.compact_widget_date_hidden),
        )
        binding.configurationDatePositionSpinner.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, datePositionOptions
        )
        val savedDatePosition = sharedPrefs.getString("widget_compact_date_position", "below") ?: "below"
        binding.configurationDatePositionSpinner.setSelection(
            datePositionValues.indexOf(savedDatePosition).coerceAtLeast(0)
        )

        val previewDatesAbove = listOf(
            binding.previewDateAbove1, binding.previewDateAbove2, binding.previewDateAbove3
        )
        val previewDatesBelow = listOf(
            binding.previewDateBelow1, binding.previewDateBelow2, binding.previewDateBelow3
        )
        updatePreviewDatePosition(previewDatesAbove, previewDatesBelow, savedDatePosition)

        val zodiacRow = binding.configurationZodiacRow
        zodiacRow.visibility = if (savedDatePosition == "hidden") android.view.View.GONE else android.view.View.VISIBLE

        binding.configurationDatePositionSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                val position = datePositionValues[pos]
                updatePreviewDatePosition(previewDatesAbove, previewDatesBelow, position)
                zodiacRow.visibility = if (position == "hidden") android.view.View.GONE else android.view.View.VISIBLE
                if (position == "hidden") binding.configurationZodiacPositionSpinner.setSelection(0)
                val zodiacPosition = zodiacPositionValues[binding.configurationZodiacPositionSpinner.selectedItemPosition]
                updatePreviewZodiacPosition(
                    getAllPreviewZodiacLists(), zodiacPosition, position
                )
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupZodiacPositionSpinner() {
        val zodiacPositionOptions = arrayOf(
            getString(R.string.compact_widget_zodiac_hidden),
            getString(R.string.compact_widget_zodiac_before_date),
            getString(R.string.compact_widget_zodiac_after_date),
        )
        binding.configurationZodiacPositionSpinner.adapter = android.widget.ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, zodiacPositionOptions
        )
        val savedZodiacPosition = sharedPrefs.getString("widget_compact_zodiac_position", "hidden") ?: "hidden"
        binding.configurationZodiacPositionSpinner.setSelection(
            zodiacPositionValues.indexOf(savedZodiacPosition).coerceAtLeast(0)
        )

        val savedDatePosition = sharedPrefs.getString("widget_compact_date_position", "below") ?: "below"
        updatePreviewZodiacPosition(getAllPreviewZodiacLists(), savedZodiacPosition, savedDatePosition)

        binding.configurationZodiacPositionSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                val zodiacPosition = zodiacPositionValues[pos]
                val datePosition = datePositionValues[binding.configurationDatePositionSpinner.selectedItemPosition]
                updatePreviewZodiacPosition(getAllPreviewZodiacLists(), zodiacPosition, datePosition)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupTextSizeSlider() {
        val savedTextSize = sharedPrefs.getInt("widget_compact_text_size", 12)
        binding.configurationTextSizeSlider.value = savedTextSize.toFloat()
        binding.configurationTextSizeValue.text = "${savedTextSize} sp"
        updatePreviewTextSize(binding.previewContent, savedTextSize.toFloat())

        binding.configurationTextSizeSlider.addOnChangeListener { _, value, _ ->
            val size = value.toInt()
            binding.configurationTextSizeValue.text = "${size} sp"
            updatePreviewTextSize(binding.previewContent, size.toFloat())
        }
    }

    private fun setupHighlightOpacitySlider() {
        val savedHighlightOpacity = sharedPrefs.getInt("widget_compact_highlight_opacity", 60)
        binding.configurationHighlightOpacitySlider.value = savedHighlightOpacity.toFloat()
        binding.configurationHighlightOpacityValue.text = "$savedHighlightOpacity%"
    }

    private fun getAllPreviewZodiacLists(): List<List<android.view.View>> {
        return listOf(
            listOf(binding.previewZodiacBeforeAbove1, binding.previewZodiacBeforeAbove2, binding.previewZodiacBeforeAbove3),
            listOf(binding.previewZodiacAfterAbove1, binding.previewZodiacAfterAbove2, binding.previewZodiacAfterAbove3),
            listOf(binding.previewZodiacBeforeBelow1, binding.previewZodiacBeforeBelow2, binding.previewZodiacBeforeBelow3),
            listOf(binding.previewZodiacAfterBelow1, binding.previewZodiacAfterBelow2, binding.previewZodiacAfterBelow3),
        )
    }

    private fun buildColorPalette(): LinkedHashMap<String, Int> {
        val names = listOf(
            "white", "black", "red", "crimson", "orange", "yellow",
            "lime", "green", "teal", "aqua", "lightBlue", "blue", "violet", "pink"
        )
        val palette = linkedMapOf<String, Int>()
        names.forEach { palette[it] = CompactWidgetRemoteViewsFactory.resolveColor(this, it) }
        return palette
    }

    private fun setupColorPickers() {
        val allColors = buildColorPalette()
        val previewContent = binding.previewContent
        val previewHighlightRow = binding.previewHighlightRow
        val slider = binding.configurationOpacitySlider
        val opacityValue = binding.configurationOpacityValue
        val highlightOpacitySlider = binding.configurationHighlightOpacitySlider
        val highlightOpacityValue = binding.configurationHighlightOpacityValue
        val allPreviewZodiacIcons = getAllPreviewZodiacLists().flatten()

        var selectedWidgetBgColor = sharedPrefs.getString("widget_compact_bg_color", "black") ?: "black"
        var selectedWidgetTextColor = sharedPrefs.getString("widget_compact_general_text_color", "white") ?: "white"
        var selectedHighlightBgColor = sharedPrefs.getString("widget_compact_highlight_color", "red") ?: "red"
        var selectedHighlightTextColor = sharedPrefs.getString("widget_compact_highlight_text_color", "white") ?: "white"

        buildColorPicker(binding.bgColorPickerContainer, allColors, selectedWidgetBgColor) { name, color ->
            selectedWidgetBgColor = name
            updatePreviewBackground(previewContent, slider.value.toInt(), color)
        }

        buildColorPicker(binding.generalTextColorPickerContainer, allColors, selectedWidgetTextColor) { name, color ->
            selectedWidgetTextColor = name
            updatePreviewGeneralTextColor(previewContent, color)
            allPreviewZodiacIcons.forEach { (it as android.widget.ImageView).setColorFilter(color) }
        }

        buildColorPicker(binding.colorPickerContainer, allColors, selectedHighlightBgColor) { name, color ->
            selectedHighlightBgColor = name
            updatePreviewHighlight(previewHighlightRow, color, highlightOpacitySlider.value.toInt())
        }

        buildColorPicker(binding.textColorPickerContainer, allColors, selectedHighlightTextColor) { name, color ->
            selectedHighlightTextColor = name
            updatePreviewHighlightText(previewHighlightRow, color)
        }

        highlightOpacitySlider.addOnChangeListener { _, value, _ ->
            val hlOpacity = value.toInt()
            highlightOpacityValue.text = "$hlOpacity%"
            val color = allColors[selectedHighlightBgColor] ?: getColor(R.color.red)
            updatePreviewHighlight(previewHighlightRow, color, hlOpacity)
        }

        slider.addOnChangeListener { _, value, _ ->
            val opacity = value.toInt()
            opacityValue.text = "$opacity%"
            val color = allColors[selectedWidgetBgColor] ?: Color.BLACK
            updatePreviewBackground(previewContent, opacity, color)
        }
    }

    private fun initializePreview() {
        val allColors = buildColorPalette()
        val selectedWidgetBgColor = sharedPrefs.getString("widget_compact_bg_color", "black") ?: "black"
        val selectedWidgetTextColor = sharedPrefs.getString("widget_compact_general_text_color", "white") ?: "white"
        val selectedHighlightBgColor = sharedPrefs.getString("widget_compact_highlight_color", "red") ?: "red"
        val selectedHighlightTextColor = sharedPrefs.getString("widget_compact_highlight_text_color", "white") ?: "white"

        val initBgColor = allColors[selectedWidgetBgColor] ?: Color.BLACK
        val initTextColor = allColors[selectedWidgetTextColor] ?: Color.WHITE
        val initHlBgColor = allColors[selectedHighlightBgColor] ?: getColor(R.color.red)
        val initHlTextColor = allColors[selectedHighlightTextColor] ?: Color.WHITE

        val savedOpacity = sharedPrefs.getInt("widget_compact_opacity", 80)
        val savedHighlightOpacity = sharedPrefs.getInt("widget_compact_highlight_opacity", 60)
        val allPreviewZodiacIcons = getAllPreviewZodiacLists().flatten()

        updatePreviewBackground(binding.previewContent, savedOpacity, initBgColor)
        updatePreviewGeneralTextColor(binding.previewContent, initTextColor)
        allPreviewZodiacIcons.forEach { (it as android.widget.ImageView).setColorFilter(initTextColor) }
        updatePreviewHighlight(binding.previewHighlightRow, initHlBgColor, savedHighlightOpacity)
        updatePreviewHighlightText(binding.previewHighlightRow, initHlTextColor)
    }

    private fun setupConfirmButton(widgetId: Int) {
        binding.configurationConfirmButton.setOnClickListener {
            sharedPrefs.edit {
                putInt("widget_compact_opacity", binding.configurationOpacitySlider.value.toInt())
                putBoolean("widget_compact_hide_images", !binding.configurationShowPhotosSwitch.isChecked)
                putInt("widget_compact_text_size", binding.configurationTextSizeSlider.value.toInt())
                putInt("widget_compact_highlight_opacity", binding.configurationHighlightOpacitySlider.value.toInt())
                putString("widget_compact_bg_color", getSelectedColorName(binding.bgColorPickerContainer))
                putString("widget_compact_general_text_color", getSelectedColorName(binding.generalTextColorPickerContainer))
                putString("widget_compact_highlight_color", getSelectedColorName(binding.colorPickerContainer))
                putString("widget_compact_highlight_text_color", getSelectedColorName(binding.textColorPickerContainer))
                putString("widget_compact_date_position", datePositionValues[binding.configurationDatePositionSpinner.selectedItemPosition])
                putString("widget_compact_zodiac_position", zodiacPositionValues[binding.configurationZodiacPositionSpinner.selectedItemPosition])
            }

            val updateIntent = Intent(this, CompactWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
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
        // Apply text color to all rows including the highlight row
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            if (row is android.view.ViewGroup) {
                setTextColorRecursive(row, color)
            }
        }
        // Restore highlight countdown text color on the first row
        val hlColorName = getSelectedColorName(binding.textColorPickerContainer)
        val hlTextColor = CompactWidgetRemoteViewsFactory.resolveColor(this, hlColorName)
        updatePreviewHighlightText(binding.previewHighlightRow, hlTextColor)
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

    private fun getSelectedColorName(container: android.widget.LinearLayout): String {
        val allColors = buildColorPalette()
        val names = allColors.keys.toList()
        for (i in 0 until container.childCount) {
            if (container.getChildAt(i).alpha == 1.0f) return names[i]
        }
        return names.first()
    }

    private fun updatePreviewTextSize(container: android.view.ViewGroup, sp: Float) {
        val smallSp = sp * CompactWidgetRemoteViewsFactory.DATE_TEXT_SCALE
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
                child.textSize = if (child.tag == "date") smallSp else sp
            } else if (child is android.view.ViewGroup) {
                updateTextSizeRecursive(child, sp, smallSp)
            }
        }
    }

    private fun updatePreviewZodiacPosition(
        zodiacLists: List<List<android.view.View>>,
        zodiacPosition: String, datePosition: String
    ) {
        val (beforeAbove, afterAbove, beforeBelow, afterBelow) = zodiacLists
        val showBefore = zodiacPosition == "before"
        val showAfter = zodiacPosition == "after"
        val showAbove = datePosition == "above"
        val showBelow = datePosition == "below"

        beforeAbove.forEach { it.visibility = if (showBefore && showAbove) android.view.View.VISIBLE else android.view.View.GONE }
        afterAbove.forEach { it.visibility = if (showAfter && showAbove) android.view.View.VISIBLE else android.view.View.GONE }
        beforeBelow.forEach { it.visibility = if (showBefore && showBelow) android.view.View.VISIBLE else android.view.View.GONE }
        afterBelow.forEach { it.visibility = if (showAfter && showBelow) android.view.View.VISIBLE else android.view.View.GONE }
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
