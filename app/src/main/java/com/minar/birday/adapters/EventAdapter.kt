package com.minar.birday.adapters

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.databinding.EventRowBinding
import com.minar.birday.databinding.MonthHeaderRowBinding
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventDataItem
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
import java.time.format.TextStyle
import java.util.*

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_EVENT = 1

@ExperimentalStdlibApi
class EventAdapter(
    private val updateFavorite: (value: EventResult) -> Unit,
    private val showFavoriteHint: () -> Unit,
    private val onItemClick: (position: Int) -> Unit,
    private val onItemLongClick: (position: Int) -> Unit
) : ListAdapter<EventDataItem, RecyclerView.ViewHolder>(EventsDiffCallback()) {
    private lateinit var context: Context
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private val adapterScope = CoroutineScope(Dispatchers.Default)

    // Return the right view type for the object, to inflate the right view holder
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EventDataItem.MonthHeader -> ITEM_VIEW_TYPE_HEADER
            is EventDataItem.EventItem -> ITEM_VIEW_TYPE_EVENT
        }
    }

    // Take the original list and divide it in months, thus adding the header
    fun addHeadersAndSubmitList(list: List<EventResult>?) {
        if (list.isNullOrEmpty()) submitList(listOf())
        else
            adapterScope.launch {
                val organizedEvents = mutableListOf<EventDataItem>()
                // Base case: insert the header for the first element and initialize the last date
                var lastDate = list[0].nextDate
                organizedEvents.add(EventDataItem.MonthHeader(lastDate!!))
                for (event in list) {
                    if (event.nextDate!!.monthValue != lastDate!!.monthValue) {
                        lastDate = event.nextDate
                        organizedEvents.add(EventDataItem.MonthHeader(lastDate))
                    }
                    organizedEvents.add(EventDataItem.EventItem(event))
                }
                activityScope.launch {
                    submitList(organizedEvents)
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        // Depending on the view type, return the correct view holder
        return when (viewType) {
            ITEM_VIEW_TYPE_EVENT -> {
                val binding = EventRowBinding
                    .inflate(LayoutInflater.from(context), parent, false)
                EventViewHolder(binding)
            }
            ITEM_VIEW_TYPE_HEADER -> {
                val binding = MonthHeaderRowBinding
                    .inflate(LayoutInflater.from(context), parent, false)
                MonthHeaderViewHolder(binding)
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MonthHeaderViewHolder -> holder.bind(getItem(position) as EventDataItem.MonthHeader)
            is EventViewHolder -> holder.bind(getItem(position) as EventDataItem.EventItem)
        }
    }

    // Can't use elsewhere without overriding as a public function
    public override fun getItem(position: Int): EventDataItem {
        return super.getItem(position)
    }

    inner class MonthHeaderViewHolder(binding: MonthHeaderRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val monthHeaderText = binding.eventDateHeader

        fun bind(monthHeader: EventDataItem.MonthHeader) {
            val headerText = "${
                monthHeader.startDate.month.getDisplayName(
                    TextStyle.FULL,
                    Locale.getDefault()
                )
            } - ${monthHeader.startDate.year}"
            monthHeaderText.text = headerText
        }
    }

    inner class EventViewHolder(binding: EventRowBinding) : RecyclerView.ViewHolder(binding.root) {
        private val favoriteButton = binding.favoriteButton
        private val fullRow = binding.root
        private val eventPerson = binding.eventPerson
        private val eventDate = binding.eventDate
        private val eventImage = binding.eventImage
        private val eventTypeImage = binding.eventTypeImage

        init {
            binding.root.setOnClickListener { onItemClick(adapterPosition) }
            binding.root.setOnLongClickListener {
                onItemLongClick(adapterPosition)
                true
            }
        }

        // Set every necessary text and click action in each row
        @ExperimentalStdlibApi
        fun bind(eventItem: EventDataItem.EventItem) {
            val event = eventItem.eventResult
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
            if (hideImages) {
                // Set the animated element name
                ViewCompat.setTransitionName(fullRow, "shared_full_view$adapterPosition")
                eventImage.visibility = View.GONE
            }
            else {
                // Set the animated element name
                ViewCompat.setTransitionName(eventImage, "shared_image$adapterPosition")
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
                        // Set the image depending on the event type
                        when (event.type) {
                            EventCode.BIRTHDAY.name -> R.drawable.placeholder_birthday_image
                            EventCode.ANNIVERSARY.name -> R.drawable.placeholder_anniversary_image
                            EventCode.DEATH.name -> R.drawable.placeholder_death_image
                            EventCode.NAME_DAY.name -> R.drawable.placeholder_name_day_image
                            else -> R.drawable.placeholder_other_image
                        }
                    )
                )
            }

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
                }
            } else eventTypeImage.visibility = View.GONE

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

class EventsDiffCallback : DiffUtil.ItemCallback<EventDataItem>() {
    override fun areItemsTheSame(oldItem: EventDataItem, newItem: EventDataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: EventDataItem, newItem: EventDataItem): Boolean {
        return oldItem == newItem
    }
}