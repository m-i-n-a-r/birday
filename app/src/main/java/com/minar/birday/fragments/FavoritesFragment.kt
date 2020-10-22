package com.minar.birday.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.facebook.shimmer.ShimmerFrameLayout
import com.minar.birday.activities.MainActivity
import com.minar.birday.R
import com.minar.birday.adapters.FavoritesAdapter
import com.minar.birday.model.EventResult
import com.minar.birday.utilities.StatsGenerator
import com.minar.birday.viewmodels.FavoritesViewModel
import kotlinx.android.synthetic.main.fragment_favorites.view.*

class FavoritesFragment : Fragment() {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var favoritesViewModel: FavoritesViewModel
    private lateinit var adapter: FavoritesAdapter
    private lateinit var fullStats: SpannableStringBuilder
    private lateinit var act: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = FavoritesAdapter()
        act = activity as MainActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v: View = inflater.inflate(R.layout.fragment_favorites, container, false)
        val statsImage = v.findViewById<ImageView>(R.id.statsImage)
        val shimmer = v.findViewById<ShimmerFrameLayout>(R.id.favoritesCardShimmer)
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        val favoritesCard = v.favoritesCard
        if (shimmerEnabled) shimmer.startShimmer()
        statsImage.applyLoopingAnimatedVectorDrawable(R.drawable.animated_candle)

        // Show full stats in a bottom sheet
        favoritesCard.setOnClickListener {
            act.vibrate()
            MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                cornerRadius(res = R.dimen.rounded_corners)
                title(R.string.stats_summary)
                icon(R.drawable.ic_stats_24dp)
                message(text = fullStats)
            }
        }
        rootView = v

        // Setup the recycler view
        initializeRecyclerView()

        favoritesViewModel = ViewModelProvider(this).get(FavoritesViewModel::class.java)
        favoritesViewModel.allFavoriteEvents.observe(viewLifecycleOwner, { events ->
            // Update the cached copy of the words in the adapter
            events?.let { adapter.submitList(it) }
        })
        favoritesViewModel.anyFavoriteEvent.observe(viewLifecycleOwner, { eventList ->
            if (eventList.isNotEmpty()) removePlaceholder()
        })
        favoritesViewModel.allEvents.observe(viewLifecycleOwner, { eventList ->
            // Under a minimum size, no stats will be shown (at least 5 events containing a year)
            if (eventList.filter { it.yearMatter == true }.size > 4) generateStat(eventList)
            else fullStats = SpannableStringBuilder(requireActivity().applicationContext.getString(
                R.string.no_stats_description
            ))
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
