package com.minar.birday.adapters

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.databinding.EventRowBinding
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getReducedDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


@ExperimentalStdlibApi
class EventAdapter(
    private val updateFavorite: (value: EventResult) -> Unit,
    private val showFavoriteHint: () -> Unit,
    private val onItemClick: (position: Int) -> Unit,
    private val onItemLongClick: (position: Int) -> Unit
) : ListAdapter<EventResult, EventAdapter.EventViewHolder>(EventsDiffCallback()) {
    private lateinit var context: Context
    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        context = parent.context
        val binding = EventRowBinding
            .inflate(LayoutInflater.from(context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Can't use elsewhere without overriding as a public function
    public override fun getItem(position: Int): EventResult {
        return super.getItem(position)
    }

    inner class EventViewHolder(binding: EventRowBinding) : RecyclerView.ViewHolder(binding.root) {
        private val favoriteButton = binding.favoriteButton
        private val eventPerson = binding.eventPerson
        private val eventDate = binding.eventDate
        private val eventImage = binding.eventImage

        init {
            binding.root.setOnClickListener { onItemClick(adapterPosition) }
            binding.root.setOnLongClickListener {
                onItemLongClick(adapterPosition)
                true
            }
        }

        // Set every necessary text and click action in each row
        @ExperimentalStdlibApi
        fun bind(event: EventResult) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val formattedPersonName =
                formatName(event, sharedPrefs.getBoolean("surname_first", false))
            // If the year isn't considered, show only the day and the month
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            val originalDate = if (event.yearMatter!!) event.originalDate.format(formatter)
            else getReducedDate(event.originalDate).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
            eventPerson.text = formattedPersonName
            eventDate.text = originalDate

            // Manage the image
            val hideImages = sharedPrefs.getBoolean("hide_images", false)
            if (hideImages) eventImage.visibility = View.GONE
            else {
                // Set a small margin programmatically
                val param = eventPerson.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(8, 0, 0, 0)
                eventPerson.layoutParams = param

                // Show and load the image, if available, or keep the placeholder
                eventImage.visibility = View.VISIBLE
                if (event.image != null && event.image.isNotEmpty()) {
                    // The click is not implemented atm
                    eventImage.setImageBitmap(byteArrayToBitmap(event.image))
                } else eventImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.placeholder_event_image
                    )
                )
            }

            // Manage the favorite logic
            if (event.favorite == false) favoriteButton.setImageResource(R.drawable.animated_to_favorite)
            else favoriteButton.setImageResource(R.drawable.animated_from_favorite)
            favoriteButton.setOnClickListener {
                if (event.favorite == true) {
                    event.favorite = false
                    activityScope.launch {
                        updateFavorite(event)
                        delay(800)
                        favoriteButton.setImageResource(R.drawable.animated_to_favorite)
                    }
                    (favoriteButton.drawable as AnimatedVectorDrawable).start()
                } else {
                    event.favorite = true
                    activityScope.launch {
                        updateFavorite(event)
                        delay(800)
                        favoriteButton.setImageResource(R.drawable.animated_from_favorite)
                    }
                    (favoriteButton.drawable as AnimatedVectorDrawable).start()
                }
            }
            favoriteButton.setOnLongClickListener {
                showFavoriteHint()
                true
            }
        }
    }
}

class EventsDiffCallback : DiffUtil.ItemCallback<EventResult>() {
    override fun areItemsTheSame(oldItem: EventResult, newItem: EventResult): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EventResult, newItem: EventResult): Boolean {
        return oldItem == newItem
    }
}