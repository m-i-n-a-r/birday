package com.minar.birday.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.minar.birday.R
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.model.EventResult
import com.minar.birday.activities.SplashActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit


class EventWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) updateAppWidget(context, appWidgetManager, appWidgetId)
    }

}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val thread = Thread {
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(context)!!.eventDao()
        val allEvents: List<EventResult> = eventDao.getOrderedAllEvents()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        val widgetUpcoming: String
        // Don't update if there's no birthday in the db
        if (allEvents.isEmpty()) return@Thread
        val event = allEvents[0]

        // Set the texts and the onclick action to open the app
        val upcomingDate = event.nextDate!!
        val nextDate = when (val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), upcomingDate).toInt()) {
            // The -1 case should never happen
            -1 -> event.nextDate.format(formatter) + ". " + context.getString(R.string.yesterday)
            0 -> event.nextDate.format(formatter) + ". " + context.getString(R.string.today)
            1 -> event.nextDate.format(formatter) + ". " + context.getString(R.string.tomorrow)
            else -> event.nextDate.format(formatter) + ". " + daysRemaining + " " + context.getString(R.string.days_left)
        }
        widgetUpcoming = if (allEvents.isEmpty()) context.getString(R.string.no_next_event)
        else allEvents[0].name + ", " + nextDate
        val views = RemoteViews(context.packageName, R.layout.event_widget)
        val intent = Intent(context, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(R.id.event_widget_main, pendingIntent)
        views.setTextViewText(R.id.event_widget_text, widgetUpcoming)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    thread.start()
}