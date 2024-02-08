package com.minar.birday.fragments.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.BottomSheetQuickAppsBinding
import com.minar.birday.utilities.applyLoopingAnimatedVectorDrawable


class QuickAppsBottomSheet(private val act: MainActivity) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetQuickAppsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the bottom sheet, initialize the shared preferences and the recent options list
        _binding = BottomSheetQuickAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Animate the drawable in loop
        val titleIcon = binding.quickAppsImage
        val whatsAppButton = binding.whatsappButton
        val messagesButton = binding.messagesButton
        val dialerButton = binding.dialerButton
        val emailButton = binding.emailButton
        val telegramButton = binding.telegramButton
        val instagramButton = binding.instagramButton
        val messengerButton = binding.messengerButton
        val viberButton = binding.viberButton
        val signalButton = binding.signalButton

        // Hide some apps if they're not installed
        if (!isAppInstalled(act, "com.whatsapp")) whatsAppButton.visibility = View.GONE
        else whatsAppButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("com.whatsapp")
            dismiss()
        }
        if (!isAppInstalled(act, "org.telegram.messenger")) telegramButton.visibility = View.GONE
        else telegramButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("org.telegram.messenger")
            dismiss()
        }
        if (!isAppInstalled(act, "com.viber.voip")) viberButton.visibility = View.GONE
        else viberButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("com.viber.voip")
            dismiss()
        }
        if (!isAppInstalled(act, "org.thoughtcrime.securesms")) signalButton.visibility = View.GONE
        else signalButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("org.thoughtcrime.securesms")
            dismiss()
        }
        if (!isAppInstalled(act, "com.instagram.android")) instagramButton.visibility = View.GONE
        else instagramButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("com.instagram.android")
            dismiss()
        }

        if (!isAppInstalled(act, "com.facebook.orca")) messengerButton.visibility = View.GONE
        else messengerButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("com.facebook.orca")
            dismiss()
        }

        titleIcon.applyLoopingAnimatedVectorDrawable(R.drawable.animated_quick_apps, 1500L)

        dialerButton.setOnClickListener {
            act.vibrate()
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                act.startActivity(dialIntent)
            } catch (e: Exception) {
                act.showSnackbar(act.getString(R.string.no_default_dialer))
            }
            dismiss()
        }

        messagesButton.setOnClickListener {
            act.vibrate()
            try {
                val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(requireContext())
                val smsIntent: Intent? =
                    act.packageManager.getLaunchIntentForPackage(defaultSmsPackage)
                act.startActivity(smsIntent)
            } catch (e: Exception) {
                act.showSnackbar(act.getString(R.string.no_default_sms))
            }
            dismiss()
        }

        emailButton.setOnClickListener {
            act.vibrate()
            try {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:")
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.wishes_birthday))
                if (intent.resolveActivity(act.packageManager) != null) {
                    startActivity(intent)
                }
            } catch (e: Exception) {
                launchOrOpenAppStore("com.google.android.gm")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }

    // Launch the selected app or open the store to prompt the user to download it
    private fun launchOrOpenAppStore(packageName: String) {
        try {
            val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            requireContext().startActivity(intent)
        } catch (e: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                )
            )
        }
    }

    // Check if an app is installed. The package name must be declared in the query field in the manifest
    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}