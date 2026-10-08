package com.minar.birday.fragments.dialogs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.BottomSheetUserBirthdayBinding
import com.minar.birday.utilities.ImageCropContract
import com.minar.birday.utilities.PREF_USER_BIRTHDAY
import com.minar.birday.utilities.PREF_USER_NAME
import com.minar.birday.utilities.deleteUserImage
import com.minar.birday.utilities.getUserBirthday
import com.minar.birday.utilities.loadUserImage
import com.minar.birday.utilities.saveUserImage
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


// Where the user enters, changes or removes their own name, birthday and picture
class UserBirthdayBottomSheet(
    private val act: MainActivity,
    private val onChanged: () -> Unit,
) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetUserBirthdayBinding? = null
    private val binding get() = _binding!!
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var cropImageLauncher: ActivityResultLauncher<Uri>
    private var birthday: LocalDate? = null
    // Only a picture chosen in this sheet is written, the saved one stays untouched otherwise
    private var newImage: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUserBirthdayBinding.inflate(inflater, container, false)
        // Same two steps as the events: pick from the gallery, then crop
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) cropImageLauncher.launch(uri)
            }
        cropImageLauncher = registerForActivityResult(ImageCropContract()) { croppedUri ->
            val bitmap = croppedUri?.let { decode(it) } ?: return@registerForActivityResult
            newImage = bitmap
            binding.userBirthdayPhoto.setImageBitmap(bitmap)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
        act.animateAvd(binding.userBirthdayTitleImage, R.drawable.animated_party_popper, 1000)

        // Start from what was saved, if anything
        val saved = getUserBirthday(act)
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(act)
        binding.userName.setText(sharedPrefs.getString(PREF_USER_NAME, "").orEmpty())
        birthday = saved?.second
        showBirthday()
        loadUserImage(act)?.let { binding.userBirthdayPhoto.setImageBitmap(it) }
        binding.userBirthdayRemove.isVisible = saved != null
        updateSaveButton()

        binding.userName.doAfterTextChanged { updateSaveButton() }
        binding.userBirthdayPhoto.setOnClickListener {
            act.vibrate()
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.userBirthday.setOnClickListener { showDatePicker() }

        binding.userBirthdaySave.setOnClickListener {
            act.vibrate()
            sharedPrefs.edit {
                putString(PREF_USER_NAME, binding.userName.text?.toString()?.trim().orEmpty())
                putString(PREF_USER_BIRTHDAY, birthday.toString())
            }
            newImage?.let { saveUserImage(act, it) }
            onChanged()
            dismiss()
        }
        binding.userBirthdayRemove.setOnClickListener {
            act.vibrate()
            sharedPrefs.edit {
                remove(PREF_USER_NAME)
                remove(PREF_USER_BIRTHDAY)
            }
            deleteUserImage(act)
            onChanged()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Both the name and the date, or there is nothing to celebrate
    private fun updateSaveButton() {
        binding.userBirthdaySave.isEnabled =
            !binding.userName.text.isNullOrBlank() && birthday != null
    }

    private fun showBirthday() {
        binding.userBirthday.setText(
            birthday?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
        )
    }

    private fun showDatePicker() {
        if (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null) return
        act.vibrate()
        // Nobody is born in the future. The picker works in UTC, so the conversion does too
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.birth_date)
            .setSelection(
                (birthday ?: LocalDate.now().minusYears(DEFAULT_AGE))
                    .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setEnd(System.currentTimeMillis())
                    .setValidator(DateValidatorPointBackward.now())
                    .build()
            )
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            birthday = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate()
            showBirthday()
            updateSaveButton()
        }
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    // BitmapFactory rather than ImageDecoder: this works below API 28 too, and gives a software
    // bitmap that can be compressed straight to the file
    private fun decode(uri: Uri): Bitmap? = try {
        act.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val DATE_PICKER_TAG = "user_birthday_picker"
        // Where the picker opens when no birthday was chosen yet
        const val DEFAULT_AGE = 25L
    }
}
