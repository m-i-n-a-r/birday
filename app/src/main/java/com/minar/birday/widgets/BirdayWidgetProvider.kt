package com.minar.birday.widgets

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.formatEventList
import com.minar.birday.utilities.getNextYears
import com.minar.birday.utilities.maxNumberOfAdditionalNotificationDays
import com.minar.birday.utilities.nextDateFormatted
import com.minar.birday.utilities.removeOrGetUpcomingEvents
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

abstract class BirdayWidgetProvider : AppWidgetProvider() {
    abstract var widgetLayout: Int

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each of the widgets with the remote adapter
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    internal fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        try {
            when (widgetLayout) {
                R.layout.widget_upcoming -> {
                    updateUpcoming(context, appWidgetManager, appWidgetId)
                }

                R.layout.widget_minimal -> {
                    updateMinimal(context, appWidgetManager, appWidgetId)
                }
            }

        } catch (e: Exception) {
            Log.d("widget", "${e.message}")
        }
    }

    // Update the minimal old widget
    private fun updateMinimal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_minimal)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val intent = Intent(context, MainActivity::class.java)
        // Retrieve previous values
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val darkText = sp.getBoolean("widget_minimal_dark_text", false)
        val background = sp.getBoolean("widget_minimal_background", false)
        val compact = sp.getBoolean("widget_minimal_compact", false)
        val alignStart = sp.getBoolean("widget_minimal_align_start", false)
        val hideIfFar = sp.getBoolean("widget_minimal_hide_if_far", false)
        val showFollowing = sp.getBoolean("widget_minimal_show_following", false)

        // First off, hide the text views and backgrounds depending on light or dark
        val titleTextView: Int
        val textTextView: Int
        if (darkText) {
            views.setViewVisibility(R.id.minimalWidgetTitleLight, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetTextLight, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetBackgroundLight, View.VISIBLE)
            views.setViewVisibility(R.id.minimalWidgetTitleDark, View.VISIBLE)
            views.setViewVisibility(R.id.minimalWidgetTextDark, View.VISIBLE)
            views.setViewVisibility(R.id.minimalWidgetBackgroundDark, View.GONE)
            titleTextView = R.id.minimalWidgetTitleDark
            textTextView = R.id.minimalWidgetTextDark
        } else {
            views.setViewVisibility(R.id.minimalWidgetTitleLight, View.VISIBLE)
            views.setViewVisibility(R.id.minimalWidgetTextLight, View.VISIBLE)
            views.setViewVisibility(R.id.minimalWidgetBackgroundLight, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetTitleDark, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetTextDark, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetBackgroundDark, View.VISIBLE)
            titleTextView = R.id.minimalWidgetTitleLight
            textTextView = R.id.minimalWidgetTextLight
        }
        // Align the text to start if selected
        if (alignStart) {
            views.setInt(R.id.minimalWidgetLinearLayout, "setGravity", Gravity.START)
        } else {
            views.setInt(R.id.minimalWidgetLinearLayout, "setGravity", Gravity.CENTER)
        }
        val hiPadding = context.resources.getDimension(R.dimen.between_row_padding).toInt()
        val loPadding = context.resources.getDimension(R.dimen.widget_margin).toInt()
        // Set the padding depending on the background
        if (background) {
            views.setViewPadding(titleTextView, hiPadding, loPadding, hiPadding, loPadding)
            views.setViewPadding(textTextView, hiPadding, 0, hiPadding, loPadding)
        } else {
            views.setViewPadding(titleTextView, loPadding, loPadding, loPadding, loPadding)
            views.setViewPadding(textTextView, loPadding, 0, loPadding, loPadding)
            views.setViewVisibility(R.id.minimalWidgetBackgroundDark, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetBackgroundLight, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetBackgroundDark, View.GONE)
            views.setViewVisibility(R.id.minimalWidgetBackgroundLight, View.GONE)
        }
        // Activate the compact layout if selected
        if (compact) {
            views.setViewVisibility(titleTextView, View.GONE)
        } else {
            views.setViewVisibility(titleTextView, View.VISIBLE)
        }

        Thread {
            // Get the next events and the proper formatter
            val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            val orderedEvents: List<EventResult> = eventDao.getOrderedEventsStatic()

            // Launch the app on click
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(R.id.minimalWidgetMain, pendingIntent)

            // Remove events in the future today (eg: now is december 1st 2023, an event has original date = december 1st 2050)
            var filteredNextEvents = removeOrGetUpcomingEvents(orderedEvents, true).toMutableList()
            filteredNextEvents.removeIf { getNextYears(it) == 0 }
            // If the events are all in the future, display them
            if (filteredNextEvents.isEmpty()) {
                filteredNextEvents = removeOrGetUpcomingEvents(orderedEvents, true).toMutableList()
            }

            // Make sure to show if there's more than one event
            var widgetUpcoming = formatEventList(filteredNextEvents, true, context, false)
            if (filteredNextEvents.isNotEmpty()) widgetUpcoming += "\n${
                nextDateFormatted(
                    filteredNextEvents[0],
                    formatter,
                    context
                )
            }"
            // Show the following event if show following is enabled
            if (showFollowing) {
                var filteredUpcomingEvents =
                    removeOrGetUpcomingEvents(orderedEvents, false).toMutableList()
                filteredUpcomingEvents =
                    removeOrGetUpcomingEvents(filteredUpcomingEvents, true).toMutableList()
                val widgetUpcomingExpanded =
                    "$widgetUpcoming \n${context.getString(R.string.next_event)} → ${
                        formatEventList(
                            filteredUpcomingEvents,
                            true,
                            context,
                            false,
                        )
                    }"
                views.setTextViewText(textTextView, widgetUpcomingExpanded)
            } else
                views.setTextViewText(textTextView, widgetUpcoming)

            // Hide the entire widget if the event is far enough in time
            if (hideIfFar) {
                val anticipationDays = maxNumberOfAdditionalNotificationDays(
                    sp.getStringSet(
                        "multi_additional_notification",
                        setOf()
                    )
                )
                if (filteredNextEvents.isEmpty() || LocalDate.now()
                        .until(filteredNextEvents.first().nextDate).days > anticipationDays
                )
                    views.setViewVisibility(R.id.minimalWidgetMain, View.INVISIBLE)
                else views.setViewVisibility(R.id.minimalWidgetMain, View.VISIBLE)
            } else views.setViewVisibility(R.id.minimalWidgetMain, View.VISIBLE)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }.start()
    }

    // Update the modern upcoming widget
    private fun updateUpcoming(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val hideImages = sp.getBoolean("hide_images", false)
        val surnameFirst = sp.getBoolean("surname_first", false)
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        val fullFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val views = RemoteViews(context.packageName, R.layout.widget_upcoming)
        val intent = Intent(context, MainActivity::class.java)

        views.setTextViewText(R.id.eventWidgetDate, fullFormatter.format(LocalDate.now()))
        Thread {
            // Get the next events and the proper formatter
            val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
            val nextEvents: List<EventResult> = eventDao.getOrderedNextEventsStatic()

            // Launch the app on click
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(R.id.background, pendingIntent)

            // If there are zero events, hide the list
            if (nextEvents.isEmpty()) {
                views.setViewVisibility(R.id.eventWidgetList, View.GONE)
                // Restore the widget to its default state
                views.setTextViewText(
                    R.id.eventWidgetText,
                    context.getString(R.string.no_next_event)
                )
                views.setImageViewResource(
                    R.id.eventWidgetImage,
                    R.drawable.placeholder_other_image
                )
            }
            // If there's one event or more, update the list and the main widget
            else {
                views.setViewVisibility(R.id.eventWidgetList, View.VISIBLE)

                // Remove events in the future today (eg: now is december 1st 2023, an event has original date = december 1st 2050)
                var filteredNextEvents = nextEvents.toMutableList()
                filteredNextEvents.removeIf { getNextYears(it) == 0 }
                // If the events are all in the future, display them but avoid confetti
                if (filteredNextEvents.isEmpty()) {
                    filteredNextEvents = nextEvents.toMutableList()
                }

                // Make sure to show if there's more than one event
                var widgetUpcoming = formatEventList(
                    filteredNextEvents,
                    surnameFirst,
                    context,
                    filteredNextEvents.size == 1
                )
                if (filteredNextEvents.isNotEmpty()) widgetUpcoming += "\n${
                    nextDateFormatted(
                        filteredNextEvents[0],
                        formatter,
                        context
                    )
                }"
                views.setTextViewText(R.id.eventWidgetText, widgetUpcoming)
                views.setTextViewText(
                    R.id.eventWidgetTitle,
                    context.getString(R.string.appwidget_upcoming)
                )

                // If the image shouldn't be shown, simply hide the view and free up space
                if (hideImages) views.setViewVisibility(R.id.eventWidgetImageGroup, View.GONE)
                // Else proceed to fill the data for the next event
                else {
                    views.setViewVisibility(R.id.eventWidgetImageGroup, View.VISIBLE)
                    if (filteredNextEvents[0].image != null && filteredNextEvents[0].image!!.isNotEmpty()) {
                        views.setImageViewBitmap(
                            R.id.eventWidgetImage,
                            byteArrayToBitmap(filteredNextEvents[0].image!!)
                        )
                    } else views.setImageViewResource(
                        R.id.eventWidgetImage,
                        // Set the image depending on the event type, the drawable are a b&w version
                        when (filteredNextEvents[0].type) {
                            EventCode.BIRTHDAY.name -> R.drawable.placeholder_birthday_image
                            EventCode.ANNIVERSARY.name -> R.drawable.placeholder_anniversary_image
                            EventCode.DEATH.name -> R.drawable.placeholder_death_image
                            EventCode.NAME_DAY.name -> R.drawable.placeholder_name_day_image
                            else -> R.drawable.placeholder_other_image
                        }
                    )
                }


                // Set up the intent that starts the EventViewService, which will provide the views
                val widgetServiceIntent = Intent(context, EventWidgetService::class.java)

                // Set up the RemoteViews object to use a RemoteViews adapter and populate the data
                views.apply {
                    setRemoteAdapter(R.id.eventWidgetList, widgetServiceIntent)
                    // setEmptyView can be used to choose the view displayed when the collection has no items
                }

                // Template to handle the click listener for each item
                val clickIntentTemplate = Intent(context, MainActivity::class.java)
                val clickPendingIntentTemplate: PendingIntent = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickIntentTemplate)
                    .getPendingIntent(
                        3,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                views.setPendingIntentTemplate(R.id.eventWidgetList, clickPendingIntentTemplate)

                // Fill the list with the next events
                appWidgetManager.notifyAppWidgetViewDataChanged(
                    appWidgetId,
                    R.id.eventWidgetList
                )
            }
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }.start()
    }
}