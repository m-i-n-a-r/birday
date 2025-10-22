package com.minar.birday.adapters

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.R
import com.minar.birday.databinding.MissedCarouselItemBinding
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.byteArrayToBitmap
import com.minar.birday.utilities.forceMonthDayFormat
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getStringForTypeCodename
import com.minar.birday.utilities.getThemeColor
import com.minar.birday.utilities.getYears


class MissedCarouselAdapter(private val missedEvents: List<EventResult>,
                            private val hideImages: Boolean = true,
                            private val onItemClick: ((EventResult) -> Unit)? = null
) :
    RecyclerView.Adapter<MissedCarouselAdapter.ViewHolder>() {
    private lateinit var context: Context

    inner class ViewHolder(binding: MissedCarouselItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val previewImage: ImageView = binding.missedCarouselImage
        val previewText: TextView = binding.missedCarouselText
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        context = viewGroup.context
        val binding =
            MissedCarouselItemBinding.inflate(LayoutInflater.from(context), viewGroup, false)
        return ViewHolder(binding)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val event = missedEvents[position]
        if (hideImages) {
            viewHolder.previewImage.setBackgroundColor(getThemeColor(R.attr.colorPrimary, context))
            val params = viewHolder.previewText.layoutParams as FrameLayout.LayoutParams
            params.apply {
                gravity = Gravity.CENTER
            }
        }
        else {
            val image = event.image
            if (image != null) viewHolder.previewImage.setImageBitmap(byteArrayToBitmap(image))
            // Set the proper image
            else {
                viewHolder.previewImage.setImageDrawable(
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
        }
        // Show a super condensed description of the event containing date, years, name and type
        val eventQuickDescription = if (event.yearMatter!!) "${
            formatName(
                event,
                false
            )
            // Show che current age, since the event is in the past
        }\n${forceMonthDayFormat(event.nextDate!!.minusYears(1))}, ${
            String.format(
                context.resources.getQuantityString(
                    R.plurals.years, getYears(event)
                ), getYears(event)
            )
        }\n${getStringForTypeCodename(context, event.type!!)}"
        // If the years can't be used, show a shorter version
        else "${
            formatName(
                event,
                false
            )
        }\n${forceMonthDayFormat(event.nextDate!!.minusYears(1))}\n${
            getStringForTypeCodename(
                context, event.type!!
            )
        }"
        viewHolder.previewText.text = eventQuickDescription

        // Delegate click to caller if provided
        viewHolder.itemView.setOnClickListener {
            onItemClick?.invoke(event)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = missedEvents.size

}
