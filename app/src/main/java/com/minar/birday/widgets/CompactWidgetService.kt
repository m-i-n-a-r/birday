package com.minar.birday.widgets

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.getCircularBitmap
import com.minar.birday.utilities.getInitialBitmap
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.getRemainingDays

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class CompactWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CompactWidgetRemoteViewsFactory(this.applicationContext)
    }
}

internal class CompactWidgetRemoteViewsFactory(private val context: Context) : RemoteViewsFactory {
    private lateinit var events: List<EventResult>
    private var surnameFirst = false
    private var hideImages = false
    private var maxRows = Int.MAX_VALUE
    private var bgAlpha = 204 // 80% of 255
    private var textSizeSp = 12f
    private var widgetBgColor = android.graphics.Color.BLACK
    private var widgetTextColor = android.graphics.Color.WHITE
    private var highlightColor = android.graphics.Color.rgb(0xFF, 0x52, 0x52) // red
    private var highlightAlpha = 153 // 60% of 255
    private var highlightTextColor = android.graphics.Color.WHITE

    override fun onCreate() {
        // In onCreate(), setup any connections / cursors to the data source
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        surnameFirst = sp.getBoolean("surname_first", false)
        hideImages = sp.getBoolean("widget_compact_hide_images", false)
        maxRows = sp.getInt("widget_compact_max_rows", Int.MAX_VALUE)
        bgAlpha = sp.getInt("widget_compact_opacity", 80) * 255 / 100
        textSizeSp = sp.getInt("widget_compact_text_size", 12).toFloat()
        widgetBgColor = resolveColor(sp.getString("widget_compact_bg_color", "black") ?: "black")
        widgetTextColor = resolveColor(sp.getString("widget_compact_general_text_color", "white") ?: "white")
        highlightAlpha = sp.getInt("widget_compact_highlight_opacity", 60) * 255 / 100
        highlightColor = resolveColor(sp.getString("widget_compact_highlight_color", "red") ?: "red")
        highlightTextColor = resolveColor(sp.getString("widget_compact_highlight_text_color", "white") ?: "white")
        events = emptyList()
    }

    override fun onDestroy() {
        // Any connection or data source must be cleared here
        events = emptyList()
    }

    override fun getCount(): Int {
        return events.size.coerceAtMost(maxRows)
    }

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_compact_row)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val event = events[position]

        // Row background with rounded corners and configured opacity
        val rowCount = count
        val bgDrawable = when {
            rowCount == 1 -> R.drawable.widget_compact_row_bg_single
            position == 0 -> R.drawable.widget_compact_row_bg_top
            position == rowCount - 1 -> R.drawable.widget_compact_row_bg_bottom
            else -> R.drawable.widget_compact_row_bg_middle
        }
        rv.setImageViewResource(R.id.compactWidgetRowBg, bgDrawable)
        rv.setInt(R.id.compactWidgetRowBg, "setColorFilter", widgetBgColor)
        rv.setInt(R.id.compactWidgetRowBg, "setImageAlpha", bgAlpha)

        // Apply configured text colors
        rv.setTextColor(R.id.compactWidgetRowName, widgetTextColor)
        rv.setTextColor(R.id.compactWidgetRowDate, widgetTextColor)
        rv.setTextColor(R.id.compactWidgetRowAge, widgetTextColor)
        rv.setTextColor(R.id.compactWidgetRowCountdown, widgetTextColor)

        // Add equal padding on edges so background has uniform inset
        val sidePadding = context.resources.getDimension(R.dimen.widget_padding).toInt()
        when {
            rowCount == 1 -> rv.setViewPadding(
                R.id.compactWidgetRowContent, sidePadding, sidePadding, sidePadding, sidePadding
            )
            position == 0 -> rv.setViewPadding(
                R.id.compactWidgetRowContent, sidePadding, sidePadding, sidePadding, 0
            )
            position == rowCount - 1 -> rv.setViewPadding(
                R.id.compactWidgetRowContent, sidePadding, 0, sidePadding, sidePadding
            )
            else -> rv.setViewPadding(
                R.id.compactWidgetRowContent, sidePadding, 0, sidePadding, 0
            )
        }

        // Apply configured text size
        val smallTextSizeSp = textSizeSp * 0.78f
        rv.setTextViewTextSize(R.id.compactWidgetRowName, android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        rv.setTextViewTextSize(R.id.compactWidgetRowDate, android.util.TypedValue.COMPLEX_UNIT_SP, smallTextSizeSp)
        rv.setTextViewTextSize(R.id.compactWidgetRowAge, android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        rv.setTextViewTextSize(R.id.compactWidgetRowCountdown, android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)

        // Scale image size to match text size (2x the text size in dp)
        val imageSizeDp = textSizeSp * 2f
        rv.setViewLayoutWidth(R.id.compactWidgetRowImage, imageSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP)
        rv.setViewLayoutHeight(R.id.compactWidgetRowImage, imageSizeDp, android.util.TypedValue.COMPLEX_UNIT_DIP)

        // Name
        rv.setTextViewText(R.id.compactWidgetRowName, formatName(event, surnameFirst))

        // Birth date
        rv.setTextViewText(
            R.id.compactWidgetRowDate,
            if (event.yearMatter!!) event.originalDate.format(formatter)
            else event.originalDate.month.getDisplayName(
                java.time.format.TextStyle.FULL, Locale.getDefault()
            ) + ", " + event.originalDate.dayOfMonth.toString()
        )

        // Age ("turns X")
        val nextYears = getNextYears(event)
        if (nextYears > 0) {
            rv.setViewVisibility(R.id.compactWidgetRowAge, View.VISIBLE)
            rv.setTextViewText(
                R.id.compactWidgetRowAge,
                context.getString(R.string.compact_widget_turns, nextYears)
            )
        } else {
            rv.setViewVisibility(R.id.compactWidgetRowAge, View.GONE)
        }

        // Countdown
        val remainingDays = getRemainingDays(event.nextDate!!)
        val countdownText = when (remainingDays) {
            0 -> context.getString(R.string.compact_widget_today)
            1 -> context.getString(R.string.compact_widget_tomorrow)
            else -> context.resources.getQuantityString(
                R.plurals.compact_widget_in_days, remainingDays, remainingDays
            )
        }
        // Bold text for today
        if (remainingDays == 0) {
            val bold = android.text.SpannableString(countdownText)
            bold.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, countdownText.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            rv.setTextViewText(R.id.compactWidgetRowCountdown, bold)
        } else {
            rv.setTextViewText(R.id.compactWidgetRowCountdown, countdownText)
        }

        // Today highlighting: colored row background + configurable countdown text color
        if (remainingDays == 0) {
            rv.setInt(R.id.compactWidgetRowBg, "setImageAlpha", highlightAlpha)
            rv.setInt(R.id.compactWidgetRowBg, "setColorFilter", highlightColor)
            rv.setTextColor(R.id.compactWidgetRowCountdown, highlightTextColor)
        }

        // Contact photo
        if (hideImages) {
            rv.setViewVisibility(R.id.compactWidgetRowImage, View.GONE)
        } else {
            rv.setViewVisibility(R.id.compactWidgetRowImage, View.VISIBLE)
            if (event.image != null && event.image!!.isNotEmpty()) {
                rv.setImageViewBitmap(
                    R.id.compactWidgetRowImage,
                    getCircularBitmap(byteArrayToBitmap(event.image!!))
                )
            } else {
                rv.setImageViewBitmap(
                    R.id.compactWidgetRowImage,
                    getInitialBitmap(event.name, event.surname, 96)
                )
            }
        }

        // Set a generic intent to open the app
        val fillInIntent = Intent()
        fillInIntent.putExtra("event", event)
        rv.setOnClickFillInIntent(R.id.compactWidgetRowItem, fillInIntent)
        // Return the remote views object
        return rv
    }

    override fun getLoadingView(): RemoteViews? {
        // Here can be specified a custom loading view. Null is the default loading view
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onDataSetChanged() {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        surnameFirst = sp.getBoolean("surname_first", false)
        hideImages = sp.getBoolean("widget_compact_hide_images", false)
        maxRows = sp.getInt("widget_compact_max_rows", Int.MAX_VALUE)
        bgAlpha = sp.getInt("widget_compact_opacity", 80) * 255 / 100
        textSizeSp = sp.getInt("widget_compact_text_size", 12).toFloat()
        widgetBgColor = resolveColor(sp.getString("widget_compact_bg_color", "black") ?: "black")
        widgetTextColor = resolveColor(sp.getString("widget_compact_general_text_color", "white") ?: "white")
        highlightAlpha = sp.getInt("widget_compact_highlight_opacity", 60) * 255 / 100
        highlightColor = resolveColor(sp.getString("widget_compact_highlight_color", "red") ?: "red")
        highlightTextColor = resolveColor(sp.getString("widget_compact_highlight_text_color", "white") ?: "white")
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        // Get all upcoming events sorted by next occurrence, including today
        events = eventDao.getOrderedEventsStatic()
    }

    private fun resolveColor(name: String): Int {
        return when (name) {
            "white" -> android.graphics.Color.WHITE
            "black" -> android.graphics.Color.BLACK
            else -> {
                val colorRes = when (name) {
                    "red" -> R.color.red
                    "crimson" -> R.color.crimson
                    "orange" -> R.color.orange
                    "yellow" -> R.color.yellow
                    "lime" -> R.color.lime
                    "green" -> R.color.green
                    "teal" -> R.color.teal
                    "aqua" -> R.color.aqua
                    "lightBlue" -> R.color.lightBlue
                    "blue" -> R.color.blue
                    "violet" -> R.color.violet
                    "pink" -> R.color.pink
                    else -> R.color.red
                }
                context.getColor(colorRes)
            }
        }
    }
}
