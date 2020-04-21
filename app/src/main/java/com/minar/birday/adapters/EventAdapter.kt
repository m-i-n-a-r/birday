package com.minar.birday.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.persistence.EventResult
import kotlinx.android.synthetic.main.event_row.view.*

class EventAdapter: BaseRecyclerViewAdapter<EventResult>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EventViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.event_row, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val eventHolder = holder as? EventViewHolder
        eventHolder?.setUpView(event = getItem(position))
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
            //favoriteButton.setImageDrawable(getDrawable(activity.context, R.drawable.animated_to_favorite))
            eventPerson.text = event?.name
            eventDate.text = event?.nextDate.toString() // TODO properly format the date
            eventYears.text = event?.nextDate.toString() // TODO properly calculate the years
        }

        override fun onClick(v: View?) {
            itemClickListener?.onItemClick(adapterPosition, v)
        }
    }
}