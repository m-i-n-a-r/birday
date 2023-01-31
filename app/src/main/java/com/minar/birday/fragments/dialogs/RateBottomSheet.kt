package com.minar.birday.fragments.dialogs

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.BottomSheetRateBinding
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable


class RateBottomSheet(private val editor: SharedPreferences.Editor) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetRateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the bottom sheet, initialize the shared preferences and the recent options list
        _binding = BottomSheetRateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Animate the drawable in loop
        val titleIcon = binding.rateImage
        titleIcon.applyLoopingAnimatedVectorDrawable(R.drawable.animated_review_star, 1500L)
        val positiveButton = binding.positiveButton
        val negativeButton = binding.negativeButton
        val neutralButton = binding.neutralButton

        // Handling the positive button
        positiveButton.setOnClickListener {
            requireContext().startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${requireContext().packageName}")
                )
            )
            editor.putBoolean(DO_NOT_SHOW_AGAIN, true)
            editor.commit()
            dismiss()
        }

        // Handling the negative button
        negativeButton.setOnClickListener { dismiss() }

        // Handling the "don't ask again" button
        neutralButton.setOnClickListener {
            editor.putBoolean(DO_NOT_SHOW_AGAIN, true)
            editor.commit()
            try {
                (context as MainActivity).showSnackbar(":'(")
            } catch (_: Exception) {
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }

    companion object {
        private const val DO_NOT_SHOW_AGAIN = "do_not_show_again"
    }
}