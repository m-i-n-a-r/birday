package com.minar.birday.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getReducedDate
import com.minar.birday.utilities.getRemainingDays
import com.minar.birday.utilities.removeOrGetUpcomingEvents
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class EventWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EventWidgetRemoteViewsFactory(this.applicationContext)
    }
}

internal class EventWidgetRemoteViewsFactory(context: Context) : RemoteViewsFactory {
    private lateinit var events: List<EventResult>
    private var surnameFirst = false
    private val context: Context

    init {
        this.context = context
    }

    override fun onCreate() {
        // In onCreate(), setup any connections / cursors to the data source
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        surnameFirst = sp.getBoolean("surname_first", false)
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
        val event = events[position]
        rv.setTextViewText(R.id.eventWidgetRowPerson, formatName(events[position], surnameFirst))
        rv.setTextViewText(
            R.id.eventWidgetRowDate,
            if (event.yearMatter!!) event.originalDate.format(formatter)
            else getReducedDate(event.originalDate).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
        )
        val remainingDays = getRemainingDays(event.nextDate!!)
        rv.setTextViewText(
            R.id.eventWidgetRowCountdown,
            if (remainingDays == 0) context.getString(R.string.exclamation) else "-$remainingDays"
        )
        // Set the image depending on the event type, the drawable are a b&w version
        rv.setImageViewResource(
            R.id.eventWidgetRowTypeImage,
            when (events[position].type) {
                EventCode.BIRTHDAY.name -> R.drawable.ic_party_24dp
                EventCode.ANNIVERSARY.name -> R.drawable.ic_anniversary_24dp
                EventCode.DEATH.name -> R.drawable.ic_death_anniversary_24dp
                EventCode.NAME_DAY.name -> R.drawable.ic_name_day_24dp
                else -> R.drawable.ic_other_24dp
            }
        )

        // Set a generic intent to open the app
        val fillInIntent = Intent()
        fillInIntent.putExtra("event", events[position])
        rv.setOnClickFillInIntent(R.id.eventWidgetRowItem, fillInIntent)
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
        val eventDao: EventDao = EventDatabase.getBirdayDatabase(context).eventDao()
        // Remove next events to avoid double data
        events = removeOrGetUpcomingEvents(eventDao.getOrderedEventsStatic())
    }
}