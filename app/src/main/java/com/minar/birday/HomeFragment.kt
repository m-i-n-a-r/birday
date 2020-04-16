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
import com.minar.birday.persistence.BirdayDatabase
import com.minar.birday.persistence.BirdayDatabase.Companion.getBirdayDataBase
import java.util.*


class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        val db: BirdayDatabase? =
            activity?.applicationContext?.let { getBirdayDataBase(it) }
        var index = 0
        val homeList: LinearLayout? = view?.findViewById(R.id.birthdayList)
        val thread = Thread {
            db?.birthdayDao()?.getBirthdays()?.sortedWith(compareBy { it.birthDate })?.forEach {
                val birthday = TextView(context)
                val concatString = it.name + " " + it.surname + " " + it.birthDate
                birthday.text = concatString
                birthday.id = index
                birthday.textSize = 15f
                birthday.setPadding(28, 18, 28, 18)
                birthday.gravity = Gravity.CENTER_HORIZONTAL
                // Ripple effect
                val outValue = TypedValue()
                Objects.requireNonNull(activity)?.theme?.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                birthday.setBackgroundResource(outValue.resourceId)
                birthday.setOnClickListener { _: View? ->
                    val optionNumber = birthday.id
                }
                homeList?.addView(birthday)
                index++
            }
        }
        /*if (sortedBirthdays. > 0) {
            val placeholder: TextView? = view?.findViewById(R.id.noBirthdays)
            homeList?.removeView(placeholder)
        }*/
            return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }
}
