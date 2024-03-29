package com.minar.birday.fragments.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.BottomSheetStatsBinding
import kotlin.math.min


class StatsBottomSheet(
    activity: MainActivity, private val totalEvents: Int,
    private val fullStats: SpannableStringBuilder
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
        // Animate the drawable in loop
        val titleIcon = binding.statsImage
        act.animateAvd(titleIcon, R.drawable.animated_stats, 1500L)

        binding.fullStats.text = fullStats
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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }
}