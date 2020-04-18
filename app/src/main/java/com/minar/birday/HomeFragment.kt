package com.minar.birday

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventDatabase.Companion.getBirdayDataBase
import java.util.*


class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        val db: EventDatabase? =
            activity?.applicationContext?.let { getBirdayDataBase(it) }
        var index = 0
        val homeList: LinearLayout? = view?.findViewById(R.id.eventList)
        val thread = Thread {
            db?.eventDao()?.getEvents()?.sortedWith(compareBy { it.originalDate })?.forEach {
                val event = TextView(context)
                val concatString = it.name + " " + it.surname + " " + it.originalDate
                event.text = concatString
                event.id = index
                event.textSize = 15f
                event.setPadding(28, 18, 28, 18)
                event.gravity = Gravity.CENTER_HORIZONTAL
                // Ripple effect
                val outValue = TypedValue()
                Objects.requireNonNull(activity)?.theme?.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                event.setBackgroundResource(outValue.resourceId)
                event.setOnClickListener { _: View? ->
                    val optionNumber = event.id
                }
                homeList?.addView(event)
                index++
            }
        }
        /*if (sortedBirthdays. > 0) {
            val placeholder: TextView? = view?.findViewById(R.id.noEvents)
            homeList?.removeView(placeholder)
        }*/
            return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
}
