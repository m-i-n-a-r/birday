package com.minar.birday.fragments.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.minar.birday.R
import com.minar.birday.databinding.BottomSheetImportContactsBinding
import com.minar.birday.persistence.ContactsRepository
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable
import com.minar.birday.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportContactsBottomSheet : BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private val contactsRepository = ContactsRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = BottomSheetImportContactsBinding.inflate(inflater, container, false).root

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = BottomSheetImportContactsBinding.bind(view)

        binding.importContactsImage.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_balloon, 1500L
        )

        binding.importContactsCancelButton.setOnClickListener { dismiss() }

        binding.importContactsConfirmButton.setOnClickListener {
            binding.importContactsButtons.visibility = View.INVISIBLE
            binding.importContactsProgressIndicator.visibility = View.VISIBLE

            // Actually import contacts
            viewLifecycleOwner.lifecycleScope.launch {
                val events = withContext(Dispatchers.IO) {
                    contactsRepository.getEventsFromContacts(requireContext().contentResolver)
                }

                viewModel.insertAll(events)

                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "import_contacts_bottom_sheet"
    }
}