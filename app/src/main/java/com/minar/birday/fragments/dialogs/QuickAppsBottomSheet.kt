package com.minar.birday.fragments.dialogs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.carousel.CarouselLayoutManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.adapters.MissedCarouselAdapter
import com.minar.birday.databinding.BottomSheetQuickAppsBinding
import java.time.Duration
import java.time.LocalDate
import androidx.core.net.toUri


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
        val threemaButton = binding.threemaButton

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
        if (!isAppInstalled(act, "ch.threema.app")) threemaButton.visibility = View.GONE
        else threemaButton.setOnClickListener {
            act.vibrate()
            launchOrOpenAppStore("ch.threema.app")
            dismiss()
        }

        act.animateAvd(titleIcon, R.drawable.animated_quick_apps, 1500L)

        dialerButton.setOnClickListener {
            act.vibrate()
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                act.startActivity(dialIntent)
            } catch (_: Exception) {
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
            } catch (_: Exception) {
                act.showSnackbar(act.getString(R.string.no_default_sms))
            }
            dismiss()
        }

        emailButton.setOnClickListener {
            act.vibrate()
            try {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = "mailto:".toUri()
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.wishes_birthday))
                if (intent.resolveActivity(act.packageManager) != null) {
                    startActivity(intent)
                }
            } catch (_: Exception) {
                launchOrOpenAppStore("com.google.android.gm")
            }
        }

        // Setup the "you might have missed" carousel
        val allEvents = act.mainViewModel.allEventsUnfiltered.value
        if (allEvents.isNullOrEmpty()) return
        val allEventsFiltered = allEvents.toMutableList()
        allEventsFiltered.removeAll {
            // Remove events not in the past 10 days
            Duration.between(
                it.nextDate!!.minusYears(1).atStartOfDay(),
                LocalDate.now().atStartOfDay()
            ).toDays() > 10
        }
        if (allEventsFiltered.isEmpty()) return

        // Ok, it makes sense to show the carousel, proceed
        val carouselTitle = binding.quickAppsMissedTitle
        val carousel = binding.quickAppsMissedCarousel
        carouselTitle.visibility = View.VISIBLE
        carousel.visibility = View.VISIBLE
        carousel.layoutManager = CarouselLayoutManager()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(act)
        val hideImages = sharedPrefs.getBoolean("hide_images", false)
        carousel.adapter = MissedCarouselAdapter(allEventsFiltered, hideImages)
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
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=${packageName}".toUri()
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