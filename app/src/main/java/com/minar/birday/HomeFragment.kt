package com.minar.birday

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Telephony
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.datetime.datePicker
import com.minar.birday.adapters.EventAdapter
import com.minar.birday.persistence.Event
import com.minar.birday.persistence.EventResult
import com.minar.birday.utilities.OnItemClickListener
import com.minar.birday.viewmodels.HomeViewModel
import kotlinx.android.synthetic.main.dialog_actions_event.view.*
import kotlinx.android.synthetic.main.dialog_apps_event.view.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*


class HomeFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var homeViewModel: HomeViewModel
    lateinit var adapter: EventAdapter
    lateinit var act: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = EventAdapter(requireActivity().applicationContext, this)
        act = activity as MainActivity
    }

    @ExperimentalStdlibApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val v: View = inflater.inflate(R.layout.fragment_home, container, false)
        val upcomingImage = v.findViewById<ImageView>(R.id.upcomingImage)
        val homeCard = v.homeCard
        upcomingImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_party_popper)

        // Open a micro app launcher
        homeCard.setOnClickListener {
            val dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(res = R.dimen.rounded_corners)
                title(R.string.event_apps)
                icon(R.drawable.ic_apps_24dp)
                message(R.string.event_apps_description)
                customView(R.layout.dialog_apps_event, scrollable = true)
            }

            val customView = dialog.getCustomView()
            // Using viewbinding to fetch the buttons
            val whatsappButton = customView.whatsappButton
            val dialerButton = customView.dialerButton
            val messagesButton = customView.messagesButton
            val telegramButton = customView.telegramButton
            val ctx: Context = requireContext()

            whatsappButton.setOnClickListener {
                act.vibrate()
                try {
                    val i: Intent? = ctx.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ctx.startActivity(i)
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp")))
                }
                dialog.dismiss()
            }

            dialerButton.setOnClickListener {
                act.vibrate()
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL)
                    ctx.startActivity(dialIntent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, ctx.getString(R.string.no_default_dialer), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            messagesButton.setOnClickListener {
                act.vibrate()
                try {
                    val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(requireContext())
                    val smsIntent: Intent? = ctx.packageManager.getLaunchIntentForPackage(defaultSmsPackage)
                    ctx.startActivity(smsIntent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, ctx.getString(R.string.no_default_sms), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            telegramButton.setOnClickListener {
                act.vibrate()
                try {
                    val i: Intent? = ctx.packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                    ctx.startActivity(i)
                } catch (e: Exception) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger")))
                }
                dialog.dismiss()
            }
        }
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
            else restorePlaceholders()
        })

        return v
    }

    // Initialize the necessary parts of the recycler view
    private fun initializeRecyclerView() {
        recyclerView = rootView.findViewById(R.id.eventRecycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    @ExperimentalStdlibApi
    private fun setUpAdapter() {
        adapter.setOnItemClickListener(onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, view: View?) {
                val act = activity as MainActivity
                val title = getString(R.string.event_actions) + " - " + adapter.getItem(position).name
                act.vibrate()
                val dialog = MaterialDialog(act).show {
                    title(text = title)
                    icon(R.drawable.ic_balloon_24dp)
                    message(R.string.event_actions_description)
                    cornerRadius(res = R.dimen.rounded_corners)
                    customView(R.layout.dialog_actions_event, scrollable = true)
                    negativeButton(R.string.cancel) {
                        dismiss()
                    }
                }

                // Setup listeners and checks on the fields
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
                    editEvent(adapter.getItem(position))
                    dialog.dismiss()
                }
            }

            override fun onItemLongClick(position: Int, view: View?): Boolean {
                act.vibrate()
                return true
            }
        })
        adapter
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val placeholder: TextView = requireView().findViewById(R.id.noEvents) ?: return
        placeholder.visibility = View.GONE
    }

    // Restore the placeholder and the default texts when the event list is empty
    private fun restorePlaceholders() {
        val cardTitle: TextView = requireView().findViewById(R.id.upcomingTitle)
        val cardSubtitle: TextView = requireView().findViewById(R.id.upcomingSubtitle)
        val cardDescription: TextView = requireView().findViewById(R.id.upcomingDescription)
        val placeholder: TextView = requireView().findViewById(R.id.noEvents)
        cardTitle.text = getString(R.string.next_event)
        cardSubtitle.text = getString(R.string.no_next_event)
        cardDescription.text = getString(R.string.no_next_event_description)
        placeholder.visibility = View.VISIBLE
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

    private fun toEvent(eventResult: EventResult) = Event(id = eventResult.id, name = eventResult.name, surname = eventResult.surname, favorite = eventResult.favorite, originalDate = eventResult.originalDate)

    @ExperimentalStdlibApi
    private fun editEvent(eventResult: EventResult) {
        var nameValue  = eventResult.name
        var surnameValue = eventResult.surname
        var eventDateValue: LocalDate = eventResult.originalDate
        val dialog = MaterialDialog(act, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            cornerRadius(res = R.dimen.rounded_corners)
            title(R.string.edit_event)
            icon(R.drawable.ic_edit_24dp)
            customView(R.layout.dialog_insert_event, scrollable = true)
            positiveButton(R.string.update_event) {
                // Use the data to create a event object and update the db
                val tuple = Event(
                    id = eventResult.id, originalDate = eventDateValue, name = nameValue.smartCapitalize(),
                    surname = surnameValue?.smartCapitalize(), favorite = eventResult.favorite
                )
                homeViewModel.update(tuple)
                dismiss()
            }
            negativeButton(R.string.cancel) {
                dismiss()
            }
        }

        // Setup listeners and checks on the fields
        dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
        val customView = dialog.getCustomView()
        val name = customView.findViewById<TextView>(R.id.nameEvent)
        val surname = customView.findViewById<TextView>(R.id.surnameEvent)
        val eventDate = customView.findViewById<TextView>(R.id.dateEvent)
        name.text = nameValue
        surname.text = surnameValue
        val formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        eventDate.text = eventDateValue.format(formatter)
        val endDate = Calendar.getInstance()
        var dateDialog: MaterialDialog? = null

        eventDate.setOnClickListener {
            // Prevent double dialogs on fast click
            if(dateDialog == null) {
                dateDialog = MaterialDialog(act).show {
                    cancelable(false)
                    cancelOnTouchOutside(false)
                    datePicker(maxDate = endDate) { _, date ->
                        val year = date.get(Calendar.YEAR)
                        val month = date.get(Calendar.MONTH) + 1
                        val day = date.get(Calendar.DAY_OF_MONTH)
                        eventDateValue = LocalDate.of(year, month, day)
                        eventDate.text = eventDateValue.format(formatter)
                    }
                }
                Handler().postDelayed({ dateDialog = null }, 750)
            }
        }

        // Validate each field in the form with the same watcher
        var nameCorrect = true
        var surnameCorrect = true
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                when {
                    editable === name.editableText -> {
                        val nameText = name.text.toString()
                        if (nameText.isBlank() || !act.checkString(nameText)) {
                            name.error = getString(R.string.invalid_value_name)
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                            nameCorrect = false
                        }
                        else {
                            nameValue = nameText
                            nameCorrect = true
                        }
                    }
                    editable === surname.editableText -> {
                        val surnameText = surname.text.toString()
                        if (!act.checkString(surnameText)) {
                            surname.error = getString(R.string.invalid_value_name)
                            dialog.getActionButton(WhichButton.POSITIVE).isEnabled = false
                            surnameCorrect = false
                        }
                        else {
                            surnameValue = surnameText
                            surnameCorrect = true
                        }
                    }
                }
                if(nameCorrect && surnameCorrect) dialog.getActionButton(WhichButton.POSITIVE).isEnabled = true
            }
        }

        name.addTextChangedListener(watcher)
        surname.addTextChangedListener(watcher)
        eventDate.addTextChangedListener(watcher)
    }

    // Extension function to quickly capitalize a name, also considering other uppercase letter or multiple words
    @ExperimentalStdlibApi
    fun String.smartCapitalize(): String =
        trim().split(" ").joinToString(" ") { it.toLowerCase(Locale.ROOT).capitalize(Locale.ROOT) }


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

