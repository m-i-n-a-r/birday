package com.minar.birday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.persistence.EventResult
import com.minar.birday.viewmodels.HomeViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class HomeFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var homeViewModel: HomeViewModel
    lateinit var adapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter(requireActivity().applicationContext)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        rootView = v

        // Setup the recycler view
        initializeRecyclerView()

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.allEvents.observe(viewLifecycleOwner, Observer { events ->
            // Update the cached copy of the words in the adapter
            events?.let { adapter.setEvents(it) }
            if (events.isNotEmpty()) insertUpcomingEvent(events.first())
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

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val homeMain: LinearLayout = requireView().findViewById(R.id.homeMain)
        val placeholder: TextView = requireView().findViewById(R.id.noEvents) ?: return
        homeMain.removeView(placeholder)
    }

    // Insert the necessary information in the upcoming event cardview TODO add other information if possible
    private fun insertUpcomingEvent(event: EventResult) {
        val cardTitle: TextView = requireView().findViewById(R.id.upcomingTitle)
        val cardSubtitle: TextView = requireView().findViewById(R.id.upcomingSubtitle)
        val cardDescription: TextView = requireView().findViewById(R.id.upcomingDescription)

        val personName = event.name + " " + event.surname
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        val nextDate = event.nextDate?.format(formatter)
        val nextAge = getString(R.string.next_age_years) + ": " + (event.nextDate?.year?.minus(event.originalDate.year)).toString()

        cardTitle.text = personName
        cardSubtitle.text = nextDate
        cardDescription.text = nextAge
    }
}
