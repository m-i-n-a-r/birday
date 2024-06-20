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
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getReducedDate
import com.minar.birday.utilities.getYears
import com.minar.birday.utilities.setEventImageOrPlaceholder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_EVENT = 1

class EventAdapter(
    private val updateFavorite: (value: EventResult) -> Unit,
    private val showFavoriteHint: () -> Unit,
    private val onItemClick: (position: Int) -> Unit,
    private val onItemLongClick: (position: Int) -> Unit,
) : ListAdapter<EventDataItem, RecyclerView.ViewHolder>(EventsDiffCallback()) {
    private lateinit var context: Context
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private val adapterScope = CoroutineScope(Dispatchers.Default)

    // Return the right view type for the object, to inflate the right view holder
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is EventDataItem.IndexHeader -> ITEM_VIEW_TYPE_HEADER
            is EventDataItem.EventItem -> ITEM_VIEW_TYPE_EVENT
        }
    }

    // Take the original list and divide it in months, thus adding the header
    fun prepareAndSubmitList(
        list: List<EventResult>?,
        orderAlphabetically: Boolean = false,
        surnameFirst: Boolean = false
    ) {
        if (list.isNullOrEmpty()) submitList(listOf())
        else adapterScope.launch {
            val organizedEvents = mutableListOf<EventDataItem>()
            // Check if the entries have to be alphabetically ordered TODO Only available in experimental settings
            if (orderAlphabetically) {
                val mutableList = list.toMutableList()
                if (surnameFirst)
                    mutableList.sortWith(compareBy({ it.surname }, { it.name }))
                else
                    mutableList.sortWith(compareBy({ it.name }, { it.surname }))
                // Base case: insert the header for the first element and initialize the first or last name letter
                var lastLetter =
                    if (mutableList.size == 0 || mutableList[0].surname.isNullOrEmpty()) "" else
                        if (surnameFirst) mutableList[0].surname?.get(0)
                            ?: "" else mutableList[0].name[0]
                organizedEvents.add(EventDataItem.IndexHeader(if (lastLetter == "") "?" else lastLetter.toString()))
                for (event in mutableList) {
                    val nextLetter =
                        if (mutableList[0].surname.isNullOrEmpty()) "" else if (surnameFirst) event.surname?.get(
                            0
                        ) ?: "" else event.name[0]
                    // If the letter has changed, add the new letter
                    if (lastLetter != nextLetter) {
                        lastLetter = nextLetter
                        organizedEvents.add(EventDataItem.IndexHeader(lastLetter.toString()))
                    }
                    organizedEvents.add(EventDataItem.EventItem(event))
                }
            }
            // Else, simply order by date (default)
            else {
                // Base case: insert the header for the first element and initialize the last date
                var lastDate = list[0].nextDate
                // Build the header title
                var headerText = "${
                    lastDate!!.month.getDisplayName(
                        TextStyle.FULL, Locale.getDefault()
                    )
                } - ${lastDate.year}"
                organizedEvents.add(EventDataItem.IndexHeader(headerText))
                for (event in list) {
                    // If the month has changed, add the new date
                    if (event.nextDate!!.monthValue != lastDate!!.monthValue ||
                        event.nextDate.year != lastDate.year
                    ) {
                        lastDate = event.nextDate
                        headerText = "${
                            lastDate.month.getDisplayName(
                                TextStyle.FULL, Locale.getDefault()
                            )
                        } - ${lastDate.year}"
                        organizedEvents.add(EventDataItem.IndexHeader(headerText))
                    }
                    organizedEvents.add(EventDataItem.EventItem(event))
                }
            }
            activityScope.launch {
                submitList(organizedEvents.toList())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        // Depending on the view type, return the correct view holder
        return when (viewType) {
            ITEM_VIEW_TYPE_EVENT -> {
                val binding = EventRowBinding.inflate(LayoutInflater.from(context), parent, false)
                EventViewHolder(binding)
            }

            ITEM_VIEW_TYPE_HEADER -> {
                val binding =
                    MonthHeaderRowBinding.inflate(LayoutInflater.from(context), parent, false)
                IndexHeaderViewHolder(binding)
            }

            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is IndexHeaderViewHolder -> holder.bind(getItem(position) as EventDataItem.IndexHeader)
            is EventViewHolder -> holder.bind(getItem(position) as EventDataItem.EventItem)
        }
    }

    // Can't use elsewhere without overriding as a public function
    public override fun getItem(position: Int): EventDataItem {
        return super.getItem(position)
    }

    inner class IndexHeaderViewHolder(binding: MonthHeaderRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val monthHeaderText = binding.eventDateHeader

        fun bind(indexHeader: EventDataItem.IndexHeader) {
            monthHeaderText.text = indexHeader.headerTitle
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
            binding.root.setOnClickListener { onItemClick(bindingAdapterPosition) }
            binding.root.setOnLongClickListener {
                onItemLongClick(bindingAdapterPosition)
                true
            }
        }

        // Set every necessary text and click action in each row
        fun bind(eventItem: EventDataItem.EventItem) {
            val event = eventItem.eventResult
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val formattedPersonName =
                formatName(event, sharedPrefs.getBoolean("surname_first", false))
            // If the year isn't considered, show only the day and the month
            val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            val originalDate = if (event.yearMatter!!) "${event.originalDate.format(formatter)} - ${
                String.format(
                    context.resources.getQuantityString(
                        R.plurals.years,
                        getYears(event)
                    ),
                    getYears(event)
                )
            }"
            else getReducedDate(event.originalDate).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
            // The original date row also has the current age
            eventPerson.text = formattedPersonName
            eventDate.text = originalDate

            // Manage the image
            val hideImages = sharedPrefs.getBoolean("hide_images", false)
            if (hideImages) {
                // Set the animated element name
                ViewCompat.setTransitionName(fullRow, "shared_full_view$bindingAdapterPosition")
                eventImage.visibility = View.GONE
            } else {
                // Set the animated element name
                ViewCompat.setTransitionName(eventImage, "shared_image$bindingAdapterPosition")
                // Set a small margin programmatically
                val param = eventPerson.layoutParams as ViewGroup.MarginLayoutParams
                param.setMargins(8, 0, 0, 0)
                eventPerson.layoutParams = param

                // Show and load the image, if available, or keep the placeholder
                eventImage.visibility = View.VISIBLE
                setEventImageOrPlaceholder(event, eventImage)
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

            // Manage the favorite/ignored logic TODO finish it
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