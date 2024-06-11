package com.minar.birday.fragments

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.adapters.FavoritesAdapter
import com.minar.birday.animators.BirdayRecyclerAnimator
import com.minar.birday.databinding.DialogNotesBinding
import com.minar.birday.databinding.FragmentFavoritesBinding
import com.minar.birday.fragments.dialogs.StatsBottomSheet
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.*
import com.minar.birday.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*


class FavoritesFragment : Fragment() {
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: FavoritesAdapter
    private lateinit var act: MainActivity
    private lateinit var sharedPrefs: SharedPreferences
    private var fullStats: SpannableStringBuilder? = null
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private var _dialogNotesBinding: DialogNotesBinding? = null
    private val dialogNotesBinding get() = _dialogNotesBinding!!
    private var totalEvents = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FavoritesAdapter(
            onItemClick = { position -> onItemClick(position) },
            onItemLongClick = { position -> onItemLongClick(position) }
        )
        act = activity as MainActivity
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Check the orientation of the screen, minimize the card on landscape
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.root.progress = 1F
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.root.progress = sharedPrefs.getFloat("favorite_motion_state", 0.0F)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val statsImage = binding.statsImage
        val shimmer = binding.favoritesCardShimmer
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val astrologyDisabled = sharedPrefs.getBoolean("disable_astrology", false)
        val favoriteMotionLayout = binding.favoritesMain
        val favoritesCard = binding.favoritesCard
        val favoritesMiniFab = binding.favoritesMiniFab
        val overviewButton = binding.overviewButton
        if (shimmerEnabled) shimmer.startShimmer()
        act.animateAvd(statsImage, R.drawable.animated_candle_new)

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

        // Activate the overscroll effect on Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.favoritesRecycler.overScrollMode = View.OVER_SCROLL_ALWAYS
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

        // Setup the recycler view
        val recycler = binding.favoritesRecycler
        recycler.adapter = adapter
        with(mainViewModel) {
            getFavorites().observe(viewLifecycleOwner) { events ->
                // Update the cached copy in the adapter
                if (events != null && events.isNotEmpty()) {
                    removePlaceholder()
                    adapter.submitList(events)
                    recycler.itemAnimator = BirdayRecyclerAnimator()
                }
            }
        }

        // Add insets
        recycler.addInsetsByPadding(bottom = true)

        // Set the overview button
        overviewButton.setOnClickListener {
            // Vibrate and navigate to the overview screen
            act.vibrate()
            requireView().findNavController()
                .navigate(R.id.action_navigationFavorites_to_overviewFragment)
        }

        // Set a tutorial snack bar on long press
        overviewButton.setOnLongClickListener {
            act.vibrate()
            act.showSnackbar(getString(R.string.overview_description))
            true
        }

        // Set the data which requires the complete and unfiltered event list
        with(binding) {
            mainViewModel.allEventsUnfiltered.observe(viewLifecycleOwner) { events ->
                // Stats - Under a minimum size, no stats will be shown (at least 5 birthdays containing a year)
                if (events.filter { it.yearMatter == true && isBirthday(it) }.size > 4)
                    generateStat(events, astrologyDisabled)
                else fullStats = SpannableStringBuilder(
                    requireActivity().applicationContext.getString(
                        R.string.no_stats_description
                    )
                )
                totalEvents = events.size

                // Quick glance - alpha set to .3 for 1 event, .6 for 2 events, 1 for 3+ events
                if (events != null) {
                    val today = LocalDate.now()
                    val nextDays = buildList {
                        for (i in 0..9L) this.add(today.plusDays(i).dayOfMonth)
                    }

                    // Prepare the dots and the UI, while resetting the opacity
                    val primary = getThemeColor(R.attr.colorPrimary, act)
                    val onPrimary = getThemeColor(R.attr.colorOnPrimary, act)
                    overviewDot1.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText1.text = nextDays[0].toString()
                    overviewDot1.alpha = .0F

                    overviewDot2.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText2.text = nextDays[1].toString()
                    overviewDot2.alpha = .0F

                    overviewDot3.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText3.text = nextDays[2].toString()
                    overviewDot3.alpha = .0F

                    overviewDot4.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText4.text = nextDays[3].toString()
                    overviewDot4.alpha = .0F

                    overviewDot5.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText5.text = nextDays[4].toString()
                    overviewDot5.alpha = .0F

                    overviewDot6.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText6.text = nextDays[5].toString()
                    overviewDot6.alpha = .0F

                    overviewDot7.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText7.text = nextDays[6].toString()
                    overviewDot7.alpha = .0F

                    overviewDot8.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText8.text = nextDays[7].toString()
                    overviewDot8.alpha = .0F

                    overviewDot9.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText9.text = nextDays[8].toString()
                    overviewDot9.alpha = .0F

                    overviewDot10.setColorFilter(primary, android.graphics.PorterDuff.Mode.SRC_IN)
                    overviewText10.text = nextDays[9].toString()
                    overviewDot10.alpha = .0F

                    // Raise the opacity for each event in that day, until events farther than 9 days
                    val alphaList: MutableList<Float> = MutableList(10) { 0F }
                    for (event in events) {
                        when (getRemainingDays(event.nextDate!!)) {
                            0 -> alphaList[0] += .30F
                            1 -> alphaList[1] += .30F
                            2 -> alphaList[2] += .30F
                            3 -> alphaList[3] += .30F
                            4 -> alphaList[4] += .30F
                            5 -> alphaList[5] += .30F
                            6 -> alphaList[6] += .30F
                            7 -> alphaList[7] += .30F
                            8 -> alphaList[8] += .30F
                            9 -> alphaList[9] += .30F
                            else -> break
                        }
                    }

                    // Remove values higher than 1, since they make no sense for alpha value
                    alphaList.forEach { it.coerceAtMost(1F) }

                    // Update the opacities
                    overviewDot1.alpha = alphaList[0]
                    overviewDot2.alpha = alphaList[1]
                    overviewDot3.alpha = alphaList[2]
                    overviewDot4.alpha = alphaList[3]
                    overviewDot5.alpha = alphaList[4]
                    overviewDot6.alpha = alphaList[5]
                    overviewDot7.alpha = alphaList[6]
                    overviewDot8.alpha = alphaList[7]
                    overviewDot9.alpha = alphaList[8]
                    overviewDot10.alpha = alphaList[9]

                    // Make sure the text is readable
                    if (overviewDot1.alpha > .7) overviewText1.setTextColor(onPrimary)
                    if (overviewDot2.alpha > .7) overviewText2.setTextColor(onPrimary)
                    if (overviewDot3.alpha > .7) overviewText3.setTextColor(onPrimary)
                    if (overviewDot4.alpha > .7) overviewText4.setTextColor(onPrimary)
                    if (overviewDot5.alpha > .7) overviewText5.setTextColor(onPrimary)
                    if (overviewDot6.alpha > .7) overviewText6.setTextColor(onPrimary)
                    if (overviewDot7.alpha > .7) overviewText7.setTextColor(onPrimary)
                    if (overviewDot8.alpha > .7) overviewText8.setTextColor(onPrimary)
                    if (overviewDot9.alpha > .7) overviewText9.setTextColor(onPrimary)
                    if (overviewDot10.alpha > .7) overviewText10.setTextColor(onPrimary)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset each binding to null to follow the best practice
        _binding = null
        _dialogNotesBinding = null
    }

    private fun onItemClick(position: Int) {
        act.vibrate()
        _dialogNotesBinding = DialogNotesBinding.inflate(LayoutInflater.from(context))
        val event = adapter.getItem(position)
        val notesTitle = "${getString(R.string.notes)} - ${event.name}"
        val noteTextField = dialogNotesBinding.favoritesNotes
        noteTextField.setText(event.notes)

        // Native dialog
        MaterialAlertDialogBuilder(act)
            .setTitle(notesTitle)
            .setIcon(R.drawable.ic_note_24dp)
            .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                val note = noteTextField.text.toString().trim()
                val tuple = Event(
                    id = event.id,
                    type = event.type,
                    originalDate = event.originalDate,
                    name = event.name,
                    yearMatter = event.yearMatter,
                    surname = event.surname,
                    favorite = event.favorite,
                    notes = note,
                    image = event.image
                )
                mainViewModel.update(tuple)
                dialog.dismiss()
            }
            .setNegativeButton(resources.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setView(dialogNotesBinding.root)
            .show()
    }

    // Open the details screen on long press, just another shortcut
    private fun onItemLongClick(position: Int) {
        // Return if there was a navigation, useful to avoid double tap on two events
        if (findNavController().currentDestination?.label != "fragment_favorites")
            return
        act.vibrate()
        // Cast required to obtain the original event result from the event item wrapper
        val event = adapter.getItem(position)

        // Navigate to the new fragment passing in the event with safe args
        val action =
            FavoritesFragmentDirections.actionNavigationFavoritesToDetailsFragment(event, position)
        findNavController().navigate(action)
    }

    // Remove the placeholder or return if the placeholder was already removed before
    private fun removePlaceholder() {
        val placeholder = binding.noFavorites
        placeholder.visibility = View.GONE
    }

    // Show a bottom sheet containing the stats
    private fun showStatsSheet() {
        act.vibrate()
        if (fullStats == null) {
            act.showSnackbar(getString(R.string.no_stats))
            return
        }
        // Prevent double dialogs in a stupid yet effective way
        for (fragment in act.supportFragmentManager.fragments) {
            if (fragment is StatsBottomSheet)
                return
        }
        val bottomSheet = StatsBottomSheet(act, totalEvents, fullStats!!)
        if (bottomSheet.isAdded) return
        bottomSheet.show(act.supportFragmentManager, "stats_bottom_sheet")
    }

    // Use the generator to generate a random stat and display it, asynchronously
    private fun generateStat(events: List<EventResult>, astrologyDisabled: Boolean = false) {
        val cardSubtitle: TextView = binding.statsSubtitle
        val cardDescription: TextView = binding.statsDescription
        val generator = StatsGenerator(events, context, astrologyDisabled)
        CoroutineScope(Dispatchers.IO).launch {
            val randomStat = generator.generateRandomStat()
            fullStats = mainViewModel.fullStats
            act.runOnUiThread {
                // Stop all UI updates if the fragment is not visible
                if (!this@FavoritesFragment.isVisible) return@runOnUiThread
                cardSubtitle.text = randomStat
                val summary = resources.getQuantityString(R.plurals.event, events.size, events.size)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                cardDescription.text = summary
            }
        }

    }
}
