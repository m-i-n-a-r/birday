package com.minar.birday.adapters

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.persistence.EventResult
import kotlinx.android.synthetic.main.event_row.view.*


class EventAdapter internal constructor(context: Context) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var events = emptyList<EventResult>() // Cached copy of events

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.event_row, parent, false))
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val current = events[position]
        holder.setUpView(event = current)
    }

    inner class EventViewHolder (view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val favoriteButton: ImageView = view.favoriteButton
        private val eventPerson: TextView = view.eventPerson
        private val eventDate: TextView = view.eventDate
        private val eventYears: TextView = view.eventYears

        init {
            view.setOnClickListener(this)
        }

        fun setUpView(event: EventResult?) {
            eventPerson.text = event?.name
            eventDate.text = event?.originalDate.toString() // TODO properly format the date
            eventYears.text = event?.nextDate.toString() // TODO properly calculate the years

            favoriteButton.setOnClickListener {
                addToFavorite(it)
                (favoriteButton.drawable as AnimatedVectorDrawable).start()
            }
        }

        override fun onClick(v: View?) {
            println("test")
        }
    }

    internal fun setEvents(events: List<EventResult>) {
        this.events = events
        notifyDataSetChanged()
    }

    override fun getItemCount() = events.size

    fun addToFavorite(v: View?) {
    }

}