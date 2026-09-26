package com.minar.birday.fragments.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.BottomSheetStatsBinding
import com.minar.birday.databinding.StatsRowBinding
import com.minar.birday.model.Stat
import com.minar.birday.utilities.CASCADE_SHEET_DELAY
import com.minar.birday.utilities.CASCADE_TIGHT_STAGGER
import com.minar.birday.utilities.animateCascade
import kotlin.math.min


class StatsBottomSheet(
    activity: MainActivity, private val totalEvents: Int,
    private val fullStats: List<Stat>
) :
    BottomSheetDialogFragment() {
    private var _binding: BottomSheetStatsBinding? = null
    private val binding get() = _binding!!
    private val act = activity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the bottom sheet, initialize the shared preferences and the recent options list
        _binding = BottomSheetStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Animate the drawable in loop
        val titleIcon = binding.statsImage
        act.animateAvd(titleIcon, R.drawable.animated_stats, 1500L)

        // One row per stat. The list is a dozen entries long and is rebuilt only when the sheet
        // opens, so a RecyclerView here would buy nothing but indirection
        val inflater = LayoutInflater.from(requireContext())
        binding.statsList.removeAllViews()
        for (stat in fullStats) {
            val row = StatsRowBinding.inflate(inflater, binding.statsList, false)
            row.statRowIcon.setImageResource(stat.icon)
            row.statRowText.text = stat.text
            binding.statsList.addView(row.root)
        }
        // Prepare the toast
        var toast: Toast? = null
        // Display the total number of events, start the animated drawable
        binding.eventCounter.text = totalEvents.toString()
        val backgroundDrawable = binding.eventCounterBackground
        // Link the opacity of the background to the number of events (min = 0.05 / max = 100)
        backgroundDrawable.alpha = min(0.01F * totalEvents + 0.05F, 1.0F)
        act.animateAvd(backgroundDrawable, R.drawable.animated_counter_background)
        // Show an explanation for the counter, even if it's quite obvious
        backgroundDrawable.setOnClickListener {
            act.vibrate()
            toast?.cancel()
            @SuppressLint("ShowToast") // The toast is shown, stupid lint
            toast = Toast.makeText(
                context, resources.getQuantityString(
                    R.plurals.stats_total,
                    totalEvents,
                    totalEvents
                ), Toast.LENGTH_LONG
            )
            toast!!.show()
        }

        // Last: the cascade lands every view on the alpha it is supposed to keep, so the counter
        // background has to have received its own before the animation reads it. The rows are part
        // of the same sequence instead of fading in as one block, which is the whole point
        buildList<View> {
            add(binding.statsImage)
            add(binding.statsTitle)
            addAll(binding.statsList.children.toList())
            add(binding.eventCounterBackground)
            add(binding.eventCounter)
        }.animateCascade(startDelay = CASCADE_SHEET_DELAY, stagger = CASCADE_TIGHT_STAGGER)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }
}