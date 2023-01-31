package com.minar.birday.fragments.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.minar.birday.R
import com.minar.birday.databinding.BottomSheetImportContactsBinding
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable

class ImportContactsBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = BottomSheetImportContactsBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = BottomSheetImportContactsBinding.bind(view)

        binding.importContactsImage.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_balloon, 1500L
        )

    }

    companion object {
        const val TAG = "import_contacts_bottom_sheet"
    }
}