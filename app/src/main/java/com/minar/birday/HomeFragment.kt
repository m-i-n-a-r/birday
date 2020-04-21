package com.minar.birday

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    lateinit var rootView: View
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateComponent()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        val db: EventDatabase? = activity?.applicationContext?.let { getBirdayDataBase(it) }
        rootView = v

        initView()
        return v
    }

    private fun onCreateComponent() {
        adapter = EventAdapter()
    }

    private fun initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.eventRecycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    private fun initView(){
        setUpAdapter()
        initializeRecyclerView()
        setUpDummyData()
    }

    private fun setUpAdapter() {
        adapter.setOnItemClickListener(onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, view: View?) {
                var user = adapter.getItem(position)
                println("test")
            }
        })
    }

    // Build a simple set of test data
    private fun setUpDummyData(){
        val list: ArrayList<EventResult> = ArrayList<EventResult>()
        list.add(EventResult(id = 0, name = "Aron", originalDate = LocalDate.parse("1995-02-23")))
        list.add(EventResult(id = 1, name = "Britney", originalDate = LocalDate.parse("1993-01-30")))
        list.add(EventResult(id = 2, name = "Clayton", originalDate = LocalDate.parse("1999-12-14")))
        list.add(EventResult(id = 3, name = "Daniel", originalDate = LocalDate.parse("1998-11-12")))
        list.add(EventResult(id = 4, name = "Esther", originalDate = LocalDate.parse("1994-03-27")))
        list.add(EventResult(id = 5, name = "Felix", originalDate = LocalDate.parse("1990-10-26")))
        list.add(EventResult(id = 6, name = "Guy", originalDate = LocalDate.parse("1991-09-21")))
        list.add(EventResult(id = 7, name = "Haden", originalDate = LocalDate.parse("2001-04-05")))
        list.add(EventResult(id = 8, name = "Jacob", originalDate = LocalDate.parse("1985-02-11")))
        adapter.addItems(list)
    }

}
