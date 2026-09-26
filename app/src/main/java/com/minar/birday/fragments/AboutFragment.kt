package com.minar.birday.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.minar.birday.BuildConfig
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.AboutRowBinding
import com.minar.birday.databinding.FragmentAboutBinding
import com.minar.birday.utilities.addNavbarClearance
import com.minar.birday.utilities.animateChildrenCascade
import com.minar.birday.utilities.getThemeColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import kotlin.time.Duration.Companion.milliseconds

/**
 * The former author preference, promoted to a page of its own: it is opened by the navbar action
 * button while the settings are on screen. No toolbar and no back arrow, the back gesture is the
 * only way out, and the framework animates it.
 */
class AboutFragment : Fragment() {
    private lateinit var act: MainActivity
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private val fragmentScope = CoroutineScope(Dispatchers.Main)

    // Easter egg stuff, why not
    private var easterEggCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        act = activity as MainActivity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        if (sharedPrefs.getBoolean("shimmer", false)) binding.settingsShimmer.startShimmer()

        // Spawn the logo with a little delay
        fragmentScope.launch {
            delay(300.milliseconds)
            (binding.imageMinar.drawable as? AnimatedVectorDrawable)?.start()
        }

        binding.imageMinar.setOnClickListener { onLogoClick() }
        binding.minarig.setOnClickListener { openLink(R.string.dev_instagram) }
        binding.minartg.setOnClickListener { openLink(R.string.dev_telegram_channel) }
        binding.minarps.setOnClickListener { openLink(R.string.dev_other_apps) }
        binding.minargit.setOnClickListener { openLink(R.string.dev_github) }
        binding.minarsite.setOnClickListener { openLink(R.string.dev_personal_site) }

        buildRows()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.aboutRows.animateChildrenCascade()
        binding.aboutScroll.addNavbarClearance()
    }

    // The rows live in code instead of in the layout: they are the same shape repeated, and the
    // language one only exists on Android 13+
    private fun buildRows() {
        addRow(
            R.drawable.ic_info_24dp,
            getString(R.string.about_version),
            BuildConfig.VERSION_NAME
        ) { openAppInfo() }
        // Per app language, backed by the locale config AGP generates from the translations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) addRow(
            R.drawable.ic_language_24dp,
            getString(R.string.about_language_title),
            getString(R.string.about_language_summary)
        ) { openLanguageSettings() }
        addRow(
            R.drawable.ic_star_24dp,
            getString(R.string.about_rate_title),
            getString(R.string.about_rate_summary)
        ) { openUrl(getString(R.string.about_store_url)) }
        addRow(
            R.drawable.ic_share_black_24dp,
            getString(R.string.about_share_title),
            getString(R.string.about_share_summary)
        ) { shareApp() }
        addRow(R.drawable.ic_privacy_24dp, getString(R.string.about_privacy_title), null) {
            val url = getString(R.string.about_privacy_url)
            if (url.isBlank()) act.showSnackbar(getString(R.string.about_privacy_missing))
            else openUrl(url)
        }
        addRow(
            R.drawable.ic_apps_email_24dp,
            getString(R.string.about_contact_title),
            getString(R.string.dev_email)
        ) { sendMail(getString(R.string.app_name), null) }
        addRow(
            R.drawable.ic_translate_24dp,
            getString(R.string.translate_app),
            getString(R.string.about_translate_summary)
        ) {
            sendMail(
                getString(R.string.translate_email_subject),
                getString(R.string.translate_email_body)
            )
        }
    }

    private fun addRow(
        @DrawableRes icon: Int,
        title: String,
        subtitle: String?,
        onClick: () -> Unit
    ) {
        val row = AboutRowBinding.inflate(layoutInflater, binding.aboutRows, false)
        row.aboutRowIcon.setImageResource(icon)
        row.aboutRowTitle.text = title
        if (subtitle.isNullOrBlank()) row.aboutRowSubtitle.visibility = View.GONE
        else row.aboutRowSubtitle.text = subtitle
        row.root.setOnClickListener {
            act.vibrate()
            onClick()
        }
        binding.aboutRows.addView(row.root)
    }

    private fun onLogoClick() {
        if (easterEggCounter < 5) {
            easterEggCounter++
            return
        }
        easterEggCounter = 0
        val confetti = binding.confettiEasterEggView
        confetti.build()
            .addColors(
                getThemeColor(R.attr.colorTertiary, act),
                getThemeColor(R.attr.colorSecondary, act),
                getThemeColor(R.attr.colorPrimary, act),
                getThemeColor(R.attr.colorOnSurface, act),
            )
            .setDirection(0.0, 359.0)
            .setSpeed(0.5f, 4f)
            .setRotationEnabled(true)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(
                Shape.DrawableShape(themedDrawable(R.drawable.ic_triangle_24dp)),
                Shape.DrawableShape(themedDrawable(R.drawable.ic_favorites_24dp)),
                Shape.DrawableShape(themedDrawable(R.drawable.ic_star_24dp)),
                Shape.DrawableShape(themedDrawable(R.drawable.ic_octagram_24dp)),
            )
            .addSizes(Size(8), Size(12), Size(16))
            // It should approximately start from the logo
            .setPosition(confetti.x + confetti.width / 2, confetti.y + confetti.height / 3)
            .burst(300)
        act.showSnackbar(getString(R.string.easter_egg))
    }

    private fun themedDrawable(@DrawableRes res: Int) = ContextCompat.getDrawable(act, res)!!

    private fun openLink(stringRes: Int) {
        act.vibrate()
        openUrl(getString(stringRes))
    }

    // Every row ends up firing an implicit intent, and any of them can be unhandled on a device
    // with no browser, no mail client or a stripped down settings app
    private fun startSafely(intent: Intent, onMissing: (() -> Unit)? = null) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            if (onMissing != null) onMissing()
            else act.showSnackbar(getString(R.string.about_no_app_found))
        }
    }

    private fun openUrl(url: String) = startSafely(Intent(Intent.ACTION_VIEW, url.toUri()))

    private fun openAppInfo() = startSafely(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${act.packageName}".toUri()
        )
    )

    // Only reachable from the row that buildRows() adds on Android 13+, but the check lives in
    // another method, so lint needs to be told where the floor is
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun openLanguageSettings() = startSafely(
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, "package:${act.packageName}".toUri())
    ) { openAppInfo() }

    private fun shareApp() = startSafely(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                .putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.about_share_text, getString(R.string.about_store_url))
                ),
            null
        )
    )

    private fun sendMail(subject: String, body: String?) = startSafely(
        Intent(Intent.ACTION_SENDTO, "mailto:${getString(R.string.dev_email)}".toUri())
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .apply { if (body != null) putExtra(Intent.EXTRA_TEXT, body) }
    )
}
