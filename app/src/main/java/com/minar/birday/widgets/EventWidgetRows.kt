package com.minar.birday.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getReducedDate
import com.minar.birday.utilities.getRemainingDays
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale


// The list of the upcoming widget, all built at once and handed to the list as a whole. It stops
// somewhere: every row travels to the launcher in one go
internal const val MAX_UPCOMING_ROWS = 50

internal class EventWidgetRows(private val context: Context) {
    private val surnameFirst =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean("surname_first", false)
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    fun items(events: List<EventResult>): RemoteViewsCompat.RemoteCollectionItems {
        val builder = RemoteViewsCompat.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(1)
        events.take(MAX_UPCOMING_ROWS).forEach { event ->
            builder.addItem(event.id.toLong(), row(event))
        }
        return builder.build()
    }

    private fun row(event: EventResult): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_row)
        rv.setTextViewText(R.id.eventWidgetRowPerson, formatName(event, surnameFirst))
        rv.setTextViewText(
            R.id.eventWidgetRowDate,
            if (event.yearMatter!!) event.originalDate.format(formatter)
            else getReducedDate(event.originalDate).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
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
            when (event.type) {
                EventCode.BIRTHDAY.name -> R.drawable.ic_party_24dp
                EventCode.ANNIVERSARY.name -> R.drawable.ic_anniversary_24dp
                EventCode.DEATH.name -> R.drawable.ic_death_anniversary_24dp
                EventCode.NAME_DAY.name -> R.drawable.ic_name_day_24dp
                else -> R.drawable.ic_other_24dp
            }
        )

        // Just the id: the whole event would weigh on the list, and the app opens its details
        val fillInIntent = Intent().putExtra(MainActivity.EXTRA_EVENT_ID, event.id)
        rv.setOnClickFillInIntent(R.id.eventWidgetRowItem, fillInIntent)
        return rv
    }
}
