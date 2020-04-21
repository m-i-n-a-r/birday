package com.minar.birday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventDatabase.Companion.getBirdayDataBase
import com.minar.birday.persistence.EventResult
import com.minar.birday.utilities.OnItemClickListener
import java.time.LocalDate


class HomeFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    lateinit var adapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        val db: EventDatabase? = activity?.applicationContext?.let { getBirdayDataBase(it) }
        rootView = v

        // Remove the placeholder
        val homeMain: LinearLayout = v.findViewById(R.id.homeMain)
        val placeholder: TextView = v.findViewById(R.id.noEvents)
        homeMain.removeView(placeholder)

        // Setup the recycler view
        initializeRecyclerView()

        return v
    }

    private fun initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.eventRecycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    private fun setUpAdapter() {
        // TODO possible row click to show details about a birthday
        /*adapter.setOnItemClickListener(onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, view: View?) {
                var event = adapter.getItem(position)
                println("test")
            }
        })*/
    }

    // Build a simple set of test data
    private fun setUpDummyData(){
        val list: ArrayList<EventResult> = ArrayList<EventResult>()
        list.add(EventResult(id = 0, name = "Aron", originalDate = LocalDate.of(1995, 2, 23)))
        list.add(EventResult(id = 1, name = "Britney", originalDate = LocalDate.of(1993,1, 30)))
        list.add(EventResult(id = 2, name = "Clayton", originalDate = LocalDate.of(1999, 12, 14)))
        list.add(EventResult(id = 3, name = "Daniel", originalDate = LocalDate.of(1998, 11, 12)))
        list.add(EventResult(id = 4, name = "Esther", originalDate = LocalDate.of(1994, 3, 27)))
        list.add(EventResult(id = 5, name = "Felix", originalDate = LocalDate.of(1990, 10, 26)))
        list.add(EventResult(id = 6, name = "Guy", originalDate = LocalDate.of(1991, 9, 21)))
        list.add(EventResult(id = 7, name = "Haden", originalDate = LocalDate.of(2001, 4, 5)))
        list.add(EventResult(id = 8, name = "Jacob", originalDate = LocalDate.of(1985, 2, 11)))
        adapter.addItems(list)
    }

}
