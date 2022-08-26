package com.minar.birday.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.formatEventList
import com.minar.birday.utilities.nextDateFormatted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@ExperimentalStdlibApi
class EventWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) updateAppWidget(context, appWidgetManager, appWidgetId)
    }
}

@ExperimentalStdlibApi
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
) {
    val thread = Thread {
        // Get the next events and the proper formatter
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        val nextEvents: List<EventResult> = eventDao.getOrderedNextEventsStatic()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

        // Make sure to show if there's more than one event
        var widgetUpcoming = formatEventList(nextEvents, true, context, false)
        if (nextEvents.isNotEmpty()) widgetUpcoming += "\n ${
            nextDateFormatted(
                nextEvents[0],
                formatter,
                context
            )
        }"

        // Set the texts and the intent (the dark/light option has been deleted)
        val views = RemoteViews(context.packageName, R.layout.event_widget)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.background, pendingIntent)
        views.setTextViewText(R.id.event_widget_text, widgetUpcoming)
        views.setTextViewText(R.id.event_widget_date, formatter.format(LocalDate.now()))
        views.setViewVisibility(R.id.event_widget_list, View.GONE)

        // If there are no events, leave the widget as is
        if (nextEvents.isEmpty()) return@Thread

        if (nextEvents[0].image != null && nextEvents[0].image!!.isNotEmpty()) {
            views.setImageViewBitmap(
                R.id.event_widget_image,
                byteArrayToBitmap(nextEvents[0].image!!)
            )
        } else views.setImageViewResource(
            R.id.event_widget_image,
            // Set the image depending on the event type, the drawable are a b&w version
            when (nextEvents[0].type) {
                EventCode.BIRTHDAY.name -> R.drawable.placeholder_birthday_image
                EventCode.ANNIVERSARY.name -> R.drawable.placeholder_anniversary_image
                EventCode.DEATH.name -> R.drawable.placeholder_death_image
                EventCode.NAME_DAY.name -> R.drawable.placeholder_name_day_image
                else -> R.drawable.placeholder_other_image
            }
        )

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    thread.start()
}
