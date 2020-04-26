package com.minar.birday

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.adapters.FavoritesAdapter
import com.minar.birday.persistence.EventResult
import com.minar.birday.utilities.StatsGenerator
import com.minar.birday.viewmodels.FavoritesViewModel

class FavoritesFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var favoritesViewModel: FavoritesViewModel
    lateinit var adapter: FavoritesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FavoritesAdapter(requireActivity().applicationContext, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v: View = inflater.inflate(R.layout.fragment_favorites, container, false)
        rootView = v

        // Setup the recycler view
        initializeRecyclerView()

        favoritesViewModel = ViewModelProvider(this).get(FavoritesViewModel::class.java)
        favoritesViewModel.allFavoriteEvents.observe(viewLifecycleOwner, Observer { events ->
            // Update the cached copy of the words in the adapter
            events?.let { adapter.setEvents(it) }
        })
        favoritesViewModel.anyFavoriteEvent.observe(viewLifecycleOwner, Observer { eventList ->
            if (eventList.isNotEmpty()) removePlaceholder()
        })
        favoritesViewModel.allEvents.observe(viewLifecycleOwner, Observer { eventList ->
            // Under a minimum size, no stats will be shown
            if (eventList.size > 4) generateStat(eventList)
        })

        return v
    }

    // Initialize the necessary parts of the recycler view
    private fun initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.favoritesRecycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val favoritesMain: LinearLayout = requireView().findViewById(R.id.favoritesMain)
        val placeholder: TextView = requireView().findViewById(R.id.noFavorites) ?: return
        favoritesMain.removeView(placeholder)
    }

    // Use the generator to generate a random stat and display it
    private fun generateStat(events: List<EventResult>) {
        val cardSubtitle: TextView = requireView().findViewById(R.id.statsSubtitle)
        val cardDescription: TextView = requireView().findViewById(R.id.statsDescription)
        val generator = StatsGenerator(events, context)
        cardSubtitle.text = generator.generateRandomStat()
        val summary = getString(R.string.stats_total) + " " + events.size + " " + getString(R.string.birthdays) + "!"
        cardDescription.text = summary
    }
}
