package com.minar.birday.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.model.EventResult
import kotlinx.android.synthetic.main.event_row.view.eventDate
import kotlinx.android.synthetic.main.event_row.view.eventPerson
import kotlinx.android.synthetic.main.favorite_row.view.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class FavoritesAdapter internal constructor(context: Context) : ListAdapter<EventResult, FavoritesAdapter.FavoriteViewHolder>(FavoritesDiffCallback()) {
    private val appContext = context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        return FavoriteViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.favorite_row, parent, false))
    }

    override fun onBindViewHolder(holder: FavoritesAdapter.FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Can't use elsewhere without overriding as a public function
    public override fun getItem(position: Int): EventResult {
        return super.getItem(position)
    }

    inner class FavoriteViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        private val eventPerson: TextView = view.eventPerson
        private val eventDate: TextView = view.eventDate
        private val eventYears: TextView = view.eventYears

        // Set every necessary text and click action in each row
        fun bind(event: EventResult?) {
            val personName = event?.name + " " + event?.surname
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

            // TODO Check if the year is considered and display the full date only if it is
            var nextDate = event?.nextDate?.format(formatter)
            if (event?.yearMatter == false) nextDate = event.nextDate?.format(formatter)

            var age = (event!!.nextDate!!.year - event.originalDate.year - 1)
            if (age < 0) age = 0
            val actualAge = appContext.getString(R.string.next_age_years) + ": " + age.toString() +
                    ", " + appContext.getString(R.string.born_in) + " " + event.originalDate.year
            eventPerson.text = personName
            eventDate.text = nextDate
            eventYears.text = actualAge
        }
    }
}

class FavoritesDiffCallback : DiffUtil.ItemCallback<EventResult>() {
    override fun areItemsTheSame(oldItem: EventResult, newItem: EventResult): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EventResult, newItem: EventResult): Boolean {
        return oldItem == newItem
    }
}