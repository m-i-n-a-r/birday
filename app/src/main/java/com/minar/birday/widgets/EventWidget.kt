package com.minar.birday.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.minar.birday.R
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventResult
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class EventWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) updateAppWidget(context, appWidgetManager, appWidgetId)
    }

}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val thread = Thread {
        val eventDao: EventDao = EventDatabase.getBirdayDataBase(context)!!.eventDao()
        val allEvents: List<EventResult> = eventDao.getOrderedAllEvents()
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val widgetUpcoming: String
        widgetUpcoming = if (allEvents.isEmpty()) context.getString(R.string.no_events)
        else allEvents[0].name + ", " + allEvents[0].nextDate?.format(formatter)
        val widgetText = context.getString(R.string.appwidget_upcoming)
        val views = RemoteViews(context.packageName, R.layout.event_widget)
        views.setTextViewText(R.id.event_widget_title, widgetText)
        views.setTextViewText(R.id.event_widget_text, widgetUpcoming)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    thread.start()
}