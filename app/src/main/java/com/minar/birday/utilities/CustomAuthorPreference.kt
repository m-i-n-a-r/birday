package com.minar.birday.utilities

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.AuthorPreferenceRowBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

@ExperimentalStdlibApi
class CustomAuthorPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs), View.OnClickListener {
    private val activityScope = CoroutineScope(Dispatchers.Main)

    // Easter egg stuff, why not
    private var easterEggCounter = 0
    private lateinit var confetti: KonfettiView

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = AuthorPreferenceRowBinding.bind(holder.itemView)

        // Manage the shimmer
        val shimmer = binding.settingsShimmer
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        if (shimmerEnabled) shimmer.startShimmer()

        // Manage confetti
        confetti = binding.confettiEasterEggView

        // Make the icons clickable
        val logo = binding.imageMinar
        val l1 = binding.minarig
        val l2 = binding.minartt
        val l3 = binding.minarps
        val l4 = binding.minargit
        val l5 = binding.minarsite

        // Spawn the logo with a little delay
        activityScope.launch {
            delay(300)
            (logo.drawable as AnimatedVectorDrawable).start()
        }
        logo.setOnClickListener(this)
        l1.setOnClickListener(this)
        l2.setOnClickListener(this)
        l3.setOnClickListener(this)
        l4.setOnClickListener(this)
        l5.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        // Vibrate using the common method in MainActivity
        val act = context as MainActivity
        val uri: Uri
        when (v.id) {
            R.id.imageMinar -> if (easterEggCounter == 5) {
                easterEggCounter = 0
                // Trigger a snackbar and confetti
                confetti.build()
                    .addColors(
                        act.getThemeColor(R.attr.colorTertiary),
                        act.getThemeColor(R.attr.colorSecondary),
                        act.getThemeColor(R.attr.colorPrimary),
                        act.getThemeColor(R.attr.colorOnSurface),
                    )
                    .setDirection(0.0, 359.0)
                    .setSpeed(0.5f, 4f)
                    .setRotationEnabled(true)
                    .setFadeOutEnabled(true)
                    .setTimeToLive(2000L)
                    .addShapes(
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_triangle_24dp
                            )!!
                        ),
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_favorites_24dp
                            )!!
                        ),
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_star_24dp
                            )!!
                        ),
                        Shape.DrawableShape(
                            ContextCompat.getDrawable(
                                act,
                                R.drawable.ic_octagram_24dp
                            )!!
                        )
                    )
                    .addSizes(Size(8), Size(12), Size(16))
                    // It should approximately start from the logo
                    .setPosition(confetti.x + confetti.width / 2, confetti.y + confetti.height / 3)
                    .burst(300)
                act.showSnackbar(context.getString(R.string.easter_egg))
            } else easterEggCounter++
            R.id.minarig -> {
                act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_instagram))
                val intent1 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent1)
            }
            R.id.minartt -> {
                act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_twitter))
                val intent2 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent2)
            }
            R.id.minarps -> {
                act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_other_apps))
                val intent3 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent3)
            }
            R.id.minargit -> {
                act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_github))
                val intent4 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent4)
            }
            R.id.minarsite -> {
                act.vibrate()
                uri = Uri.parse(context.getString(R.string.dev_personal_site))
                val intent5 = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent5)
            }
        }
    }
}