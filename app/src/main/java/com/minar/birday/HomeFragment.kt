package com.minar.birday

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.persistence.Event
import com.minar.birday.persistence.EventResult
import com.minar.birday.utilities.OnItemClickListener
import com.minar.birday.viewmodels.HomeViewModel
import kotlinx.android.synthetic.main.dialog_actions_event.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit


class HomeFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var homeViewModel: HomeViewModel
    lateinit var adapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter(requireActivity().applicationContext, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        val upcomingImage = v.findViewById<ImageView>(R.id.upcomingImage)
            upcomingImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_party_popper)
        rootView = v

        // Setup the recycler view
        initializeRecyclerView()
        setUpAdapter()

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.allEvents.observe(viewLifecycleOwner, Observer { events ->
            // Update the cached copy of the words in the adapter
            events?.let { adapter.setEvents(it) }
            if (events.isNotEmpty()) insertUpcomingEvents(events)
        })
        homeViewModel.anyEvent.observe(viewLifecycleOwner, Observer { eventList ->
            if (eventList.isNotEmpty()) removePlaceholder()
        })

        return v
    }

    // Initialize the necessary parts of the recycler view
    private fun initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.eventRecycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    private fun setUpAdapter() {
        adapter.setOnItemClickListener(onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, view: View?) {
                val act = activity as MainActivity
                act.vibrate()
                val dialog = MaterialDialog(act).show {
                    title(R.string.event_actions)
                    icon(R.drawable.ic_balloon_24dp)
                    message(R.string.event_actions_description)
                    cornerRadius(res = R.dimen.rounded_corners)
                    customView(R.layout.dialog_actions_event, scrollable = true)
                    negativeButton(R.string.cancel) {
                        dismiss()
                    }
                }

                // Setup listeners and checks on the fields
                dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                val customView = dialog.getCustomView()
                // Using viewbinding to fetch the buttons
                val deleteButton = customView.deleteButton
                val editButton = customView.editButton

                deleteButton.setOnClickListener {
                    act.vibrate()
                    deleteEvent(adapter.getItem(position))
                    dialog.dismiss()
                }

                editButton.setOnClickListener {
                    act.vibrate()
                    dialog.dismiss()
                }
            }

            override fun onItemLongClick(position: Int, view: View?): Boolean {
                val act = activity as MainActivity
                act.vibrate()
                return true
            }
        })
        adapter
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val homeMain: LinearLayout = requireView().findViewById(R.id.homeMain)
        val placeholder: TextView = requireView().findViewById(R.id.noEvents) ?: return
        homeMain.removeView(placeholder)
    }

    // Insert the necessary information in the upcoming event cardview
    private fun insertUpcomingEvents(events: List<EventResult>) {
        val cardTitle: TextView = requireView().findViewById(R.id.upcomingTitle)
        val cardSubtitle: TextView = requireView().findViewById(R.id.upcomingSubtitle)
        val cardDescription: TextView = requireView().findViewById(R.id.upcomingDescription)
        var personName = ""
        var nextDateText = ""
        var nextAge = ""
        val upcomingDate = events[0].nextDate

        // Manage multiple events in the same day considering first case, middle cases and last case if more than 3
        for (event in events) {
            if (event.nextDate!!.isEqual(upcomingDate)) {
                val actualPersonName = if (event.surname.isNullOrBlank()) event.name
                else event.name + " " + event.surname
                when (events.indexOf(event)) {
                    0 -> {
                        personName = actualPersonName
                        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                        nextDateText = when (val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), upcomingDate).toInt()) {
                            0 -> event.nextDate.format(formatter) + ". " + getString(R.string.today) + "!"
                            1 -> event.nextDate.format(formatter) + ". " + getString(R.string.tomorrow) +"!"
                            else -> event.nextDate.format(formatter) + ". " + daysRemaining + " " + getString(R.string.days_left)

                        }
                        nextAge = getString(R.string.next_age_years) + ": " + (event.nextDate.year.minus(event.originalDate.year)).toString()
                    }
                    1, 2 -> {
                        personName += ", $actualPersonName"
                        nextAge += ", " + (event.nextDate.year.minus(event.originalDate.year)).toString()
                    }
                    3 -> {
                        personName += " " + getString(R.string.event_others)
                        nextAge += "..."
                    }
                }
            }
            if (!event.nextDate.isEqual(upcomingDate)) break
        }

        cardTitle.text = personName
        cardSubtitle.text = nextDateText
        cardDescription.text = nextAge
    }

    // Functions to update, delete and create an Event object to pass instead of the returning object passed
    fun updateFavorite(eventResult: EventResult) = homeViewModel.update(toEvent(eventResult))

    fun deleteEvent(eventResult: EventResult) = homeViewModel.delete(toEvent(eventResult))

    private fun toEvent(event: EventResult) = Event(id = event.id, name = event.name, surname = event.surname, favorite = event.favorite, originalDate = event.originalDate)

    // Loop the animated vector drawable
    internal fun ImageView.applyLoopingAnimatedVectorDrawable(@DrawableRes animatedVector: Int) {
        val animated = AnimatedVectorDrawableCompat.create(context, animatedVector)
        animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                this@applyLoopingAnimatedVectorDrawable.post { animated.start() }
            }
        })
        this.setImageDrawable(animated)
        animated?.start()
    }
}

