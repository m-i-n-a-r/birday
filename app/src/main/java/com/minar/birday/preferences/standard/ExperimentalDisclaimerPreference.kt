package com.minar.birday.preferences.standard

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.BuildConfig
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.ExperimentalDisclaimerRowBinding


class ExperimentalDisclaimerPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = ExperimentalDisclaimerRowBinding.bind(holder.itemView)

        // Animate the vector drawable
        (context as MainActivity).animateAvd(
            binding.animatedDanger,
            R.drawable.animated_experimental_danger, 1500
        )

        // Set the "open Github develop branch" button
        binding.checkGithubButton.setOnClickListener {
            (context as MainActivity).vibrate()
            val uri = context.getString(R.string.github_repo_develop).toUri()
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        // Set the "translate the app" button
        binding.translateButton.setOnClickListener {
            (context as MainActivity).vibrate()
            val uri = context.getString(R.string.crowdin_project).toUri()
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        // Set the app version name and number text
        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        val versionInfo = "v$versionName ($versionCode)"
        binding.appVersionInfo.text = versionInfo
    }
}