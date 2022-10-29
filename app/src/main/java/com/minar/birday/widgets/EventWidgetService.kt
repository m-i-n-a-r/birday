package com.minar.birday.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.minar.birday.R
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class EventWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EventWidgetRemoteViewsFactory(this.applicationContext)
    }
}

internal class EventWidgetRemoteViewsFactory(context: Context) : RemoteViewsFactory {
    private lateinit var events: List<EventResult>
    private val context: Context

    init {
        this.context = context
    }

    override fun onCreate() {
        // In onCreate() you setup any connections / cursors to your data source
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        events = eventDao.getOrderedNextEventsStatic()
    }

    override fun onDestroy() {
        // Any connection or data source must be cleared here
        events = emptyList()
    }

    override fun getCount(): Int {
        return events.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        // Any loading in this part is legitimate
        val rv = RemoteViews(context.packageName, R.layout.widget_row)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        rv.setTextViewText(R.id.eventWidgetRowPerson, events[position].name)
        rv.setTextViewText(R.id.eventWidgetRowDate, events[position].originalDate.format(formatter))
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
        println("DATASET CHANGEDDDD!!!!!!!!")
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        events = eventDao.getOrderedNextEventsStatic()
    }
}