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
import com.minar.birday.viewmodels.HomeViewModel


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

        // Remove the placeholder
        val homeMain: LinearLayout = v.findViewById(R.id.homeMain)
        val placeholder: TextView = v.findViewById(R.id.noEvents)
        homeMain.removeView(placeholder)

        // Setup the recycler view
        initializeRecyclerView()

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.allEvents.observe(viewLifecycleOwner, Observer { events ->
            // Update the cached copy of the words in the adapter.
            events?.let { adapter.setEvents(it) }
        })

        return v
    }

    // Initialize the necessary parts of the recycler view
    private fun initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.eventRecycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

}
