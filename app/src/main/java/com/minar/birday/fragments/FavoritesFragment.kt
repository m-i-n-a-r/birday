package com.minar.birday.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.adapters.FavoritesAdapter
import com.minar.birday.databinding.DialogNotesBinding
import com.minar.birday.databinding.DialogStatsBinding
import com.minar.birday.databinding.FragmentFavoritesBinding
import com.minar.birday.listeners.OnItemClickListener
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.StatsGenerator
import com.minar.birday.viewmodels.MainViewModel
import kotlin.math.min


class FavoritesFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var mainViewModel: MainViewModel
    private lateinit var adapter: FavoritesAdapter
    private lateinit var fullStats: SpannableStringBuilder
    private lateinit var act: MainActivity
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private var _dialogStatsBinding: DialogStatsBinding? = null
    private val dialogStatsBinding get() = _dialogStatsBinding!!
    private var _dialogNotesBinding: DialogNotesBinding? = null
    private val dialogNotesBinding get() = _dialogNotesBinding!!
    private var totalEvents = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FavoritesAdapter()
        act = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val v = binding.root
        val statsImage = binding.statsImage
        val shimmer = binding.favoritesCardShimmer
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val favoriteMotionLayout = binding.favoritesMain
        val favoritesCard = binding.favoritesCard
        val favoritesMiniFab = binding.favoritesMiniFab
        if (shimmerEnabled) shimmer.startShimmer()
        statsImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_candle)

        // Set motion layout state, since it's saved
        favoriteMotionLayout.progress = sharedPrefs.getFloat("favorite_motion_state", 0.0F)

        // Vibration on the mini fab (with manual managing of the transition)
        favoritesMiniFab.setOnClickListener {
            act.vibrate()
            when (favoriteMotionLayout.progress) {
                0.0F -> {
                    favoriteMotionLayout.transitionToEnd()
                    sharedPrefs.edit().putFloat("favorite_motion_state", 1.0F).apply()
                }
                1.0F -> {
                    favoriteMotionLayout.transitionToStart()
                    sharedPrefs.edit().putFloat("favorite_motion_state", 0.0F).apply()
                }
            }
        }

        // Show full stats on long press too
        favoritesMiniFab.setOnLongClickListener {
            if (favoriteMotionLayout.progress == 1.0F) showStatsSheet()
            true
        }

        // Show full stats in a bottom sheet
        favoritesCard.setOnClickListener {
            showStatsSheet()
        }
        rootView = v

        // Setup the recycler view
        initializeRecyclerView()
        setUpAdapter()

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        mainViewModel.allFavoriteEvents.observe(viewLifecycleOwner, { events ->
            // Update the cached copy of the words in the adapter
            if (events != null && events.isNotEmpty()) {
                removePlaceholder()
                adapter.submitList(events)
            }
        })
        // AllEvents contains everything, since the query string is reset when the fragment changes
        mainViewModel.allEvents.observe(viewLifecycleOwner, { eventList ->
            // Under a minimum size, no stats will be shown (at least 5 events containing a year)
            if (eventList.filter { it.yearMatter == true }.size > 4) generateStat(eventList)
            else fullStats = SpannableStringBuilder(
                requireActivity().applicationContext.getString(
                    R.string.no_stats_description
                )
            )
            totalEvents = eventList.size
        })

        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset each binding to null to follow the best practice
        _binding = null
        _dialogStatsBinding = null
        _dialogNotesBinding = null
    }

    // Initialize the necessary parts of the recycler view
    private fun initializeRecyclerView() {
        recyclerView = binding.favoritesRecycler
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
    }

    // Manage the onclick actions, or the long click (unused atm)
    private fun setUpAdapter() {
        adapter.setOnItemClickListener(onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(position: Int, view: View?) {
                act.vibrate()
                val event = adapter.getItem(position)
                val title = getString(R.string.notes) + " - " + event.name
                MaterialDialog(act).show {
                    title(text = title)
                    icon(R.drawable.ic_note_24dp)
                    cornerRadius(res = R.dimen.rounded_corners)
                    // Get the text field and set the view using the binding
                    _dialogNotesBinding = DialogNotesBinding.inflate(LayoutInflater.from(context))
                    customView(view = dialogNotesBinding.root, scrollable = true)
                    val noteTextField = dialogNotesBinding.favoritesNotes
                    noteTextField.setText(event.notes)
                    negativeButton(R.string.cancel) {
                        dismiss()
                    }
                    positiveButton {
                        val note = noteTextField.text.toString().trim()
                        val tuple = Event(
                            id = event.id,
                            originalDate = event.originalDate,
                            name = event.name,
                            yearMatter = event.yearMatter,
                            surname = event.surname,
                            favorite = event.favorite,
                            notes = note,
                            image = event.image
                        )
                        mainViewModel.update(tuple)
                        dismiss()
                    }
                }
            }

            // TODO reassign an action to the long press
            override fun onItemLongClick(position: Int, view: View?): Boolean {
                return true
            }

        })
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val placeholder = binding.noFavorites
        placeholder.visibility = View.GONE
    }

    // Show a bottom sheet containing the stats
    private fun showStatsSheet() {
        act.vibrate()
        _dialogStatsBinding = DialogStatsBinding.inflate(LayoutInflater.from(context))
        MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            cornerRadius(res = R.dimen.rounded_corners)
            title(R.string.stats_summary)
            icon(R.drawable.ic_stats_24dp)
            // Don't use scrollable here, instead use a nestedScrollView in the layout
            customView(view = dialogStatsBinding.root)
        }
        dialogStatsBinding.fullStats.text = fullStats
        // Display the total number of birthdays, start the animated drawable
        dialogStatsBinding.eventCounter.text = totalEvents.toString()
        val backgroundDrawable = dialogStatsBinding.eventCounterBackground
        // Link the opacity of the background to the number of events (min = 0.05 / max = 100)
        backgroundDrawable.alpha = min(0.01F * totalEvents + 0.05F, 1.0F)
        backgroundDrawable.applyLoopingAnimatedVectorDrawable(R.drawable.animated_counter_background)
        // Show an explanation for the counter, even if it's quite obvious
        backgroundDrawable.setOnClickListener {
            act.vibrate()
            Toast.makeText(
                requireContext(),
                resources.getQuantityString(R.plurals.stats_total, totalEvents, totalEvents),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Use the generator to generate a random stat and display it
    private fun generateStat(events: List<EventResult>) {
        val cardSubtitle: TextView = binding.statsSubtitle
        val cardDescription: TextView = binding.statsDescription
        val generator = StatsGenerator(events, context)
        cardSubtitle.text = generator.generateRandomStat()
        fullStats = generator.generateFullStats()
        val summary = resources.getQuantityString(R.plurals.stats_total, events.size, events.size)
        cardDescription.text = summary
    }

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
