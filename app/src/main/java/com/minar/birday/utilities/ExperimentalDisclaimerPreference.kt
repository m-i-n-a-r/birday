package com.minar.birday.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.ExperimentalDisclaimerRowBinding

@ExperimentalStdlibApi
class ExperimentalDisclaimerPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = ExperimentalDisclaimerRowBinding.bind(holder.itemView)

        // Animate the vector drawable
        binding.animatedDanger.applyLoopingAnimatedVectorDrawable(
            R.drawable.animated_experimental_danger, 1500
        )

        // Set the "open Github develop branch"
        binding.checkGithubButton.setOnClickListener {
            (context as MainActivity).vibrate()
            val uri = Uri.parse(context.getString(R.string.github_repo_develop))
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}