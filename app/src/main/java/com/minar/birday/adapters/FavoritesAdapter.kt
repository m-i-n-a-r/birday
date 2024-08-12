package com.minar.birday.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.databinding.FavoriteRowBinding
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getRemainingDays
import com.minar.birday.utilities.getYears
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class FavoritesAdapter(
    private val onItemClick: (position: Int) -> Unit,
    private val onItemLongClick: (position: Int) -> Unit
) : ListAdapter<EventResult, FavoritesAdapter.FavoriteViewHolder>(FavoritesDiffCallback()) {
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        context = parent.context
        val binding = FavoriteRowBinding
            .inflate(LayoutInflater.from(context), parent, false)
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoritesAdapter.FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Can't use elsewhere without overriding as a public function
    public override fun getItem(position: Int): EventResult {
        return super.getItem(position)
    }

    inner class FavoriteViewHolder(binding: FavoriteRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val eventPerson = binding.eventPerson
        private val eventNote = binding.eventNote
        private val eventDate = binding.eventDate
        private val eventYears = binding.eventYears
        private val eventTypeImage = binding.eventTypeImage
        private val eventCountdown = binding.eventCountdown

        init {
            binding.root.setOnClickListener { onItemClick(bindingAdapterPosition) }
            binding.root.setOnLongClickListener {
                onItemLongClick(bindingAdapterPosition)
                true
            }
        }

        // Set every necessary text and click action in each row
        fun bind(event: EventResult) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val formattedPersonName =
                formatName(event, sharedPrefs.getBoolean("surname_first", false))
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            val age = getYears(event)
            val daysRemaining = getRemainingDays(event.nextDate!!)
            val daysCountdown = if (daysRemaining > 0) "-$daysRemaining"
            else context.getString(R.string.exclamation)
            var nextDate = event.nextDate.format(formatter)

            if (event.yearMatter == false) nextDate = event.nextDate.format(formatter)
            val actualAge = if (event.type == EventCode.BIRTHDAY.name)
                "${context.getString(R.string.next_age_years)}: $age, ${context.getString(R.string.born_in)} ${event.originalDate.year}"
            else "${context.getString(R.string.next_age_years)}: $age"
            when (event.type) {
                context.getString(R.string.vehicle_insurance_caps) -> {
                    eventPerson.text= event.manufacturerName.toString()
                }
                context.getString(R.string.vehicle_insurance_caps) -> {
                    eventPerson.text= event.input1.toString()
                }
                else -> {
                    eventPerson.text = formattedPersonName
                }
            }
            // Show an icon if there's a note
            if (!event.notes.isNullOrEmpty()) eventNote.visibility = View.VISIBLE
            else eventNote.visibility = View.GONE
            eventDate.text = nextDate
            eventCountdown.text = daysCountdown

            // Hide the age row if the year doesn't matter
            if (event.yearMatter == true) {
                eventYears.visibility = View.VISIBLE
                eventYears.text = actualAge
            } else eventYears.visibility = View.GONE

            // Manage the event type icon
            if (event.type != EventCode.BIRTHDAY.name) {
                eventTypeImage.visibility = View.VISIBLE
                when (event.type) {
                    EventCode.ANNIVERSARY.name -> eventTypeImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            context, R.drawable.ic_anniversary_24dp
                        )
                    )
                    EventCode.DEATH.name -> eventTypeImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            context, R.drawable.ic_death_anniversary_24dp
                        )
                    )
                    EventCode.NAME_DAY.name -> eventTypeImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            context, R.drawable.ic_name_day_24dp
                        )
                    )
                    EventCode.OTHER.name -> eventTypeImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            context, R.drawable.ic_other_24dp
                        )
                    )
                    EventCode.VEHICLE_INSURANCE.name -> eventTypeImage.visibility=View.GONE
                    EventCode.VEHICLE_INSURANCE_RENEWAL.name -> eventTypeImage.visibility=View.GONE

                }
            } else eventTypeImage.visibility = View.GONE
        }
    }
}

class FavoritesDiffCallback : DiffUtil.ItemCallback<EventResult>() {
    override fun areItemsTheSame(oldItem: EventResult, newItem: EventResult): Boolean {
        return oldItem.id == newItem.id
    }

    // Consider the notes, which is not in the equals method but is important to trigger the icon
    override fun areContentsTheSame(oldItem: EventResult, newItem: EventResult): Boolean {
        return oldItem == newItem && oldItem.notes == newItem.notes
    }
}