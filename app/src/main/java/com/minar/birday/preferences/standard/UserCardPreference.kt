package com.minar.birday.preferences.standard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.UserCardRowBinding
import com.minar.birday.fragments.dialogs.UserBirthdayBottomSheet
import com.minar.birday.utilities.formatDaysRemaining
import com.minar.birday.utilities.getUserBirthday
import com.minar.birday.utilities.loadUserImage
import com.minar.birday.utilities.nextUserBirthday
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit


// Switching between the empty and the filled card, long enough to be seen as one card changing
private const val CARD_CHANGE_DURATION = 350L

// The user's own birthday: an invitation when empty, a compact read only card once saved
class UserCardPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private var binding: UserCardRowBinding? = null
    // Whether the card shines today. Kept apart from the view, which the list detaches and
    // attaches again: the shimmer stops on every detach and would not come back by itself
    private var shine = false

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val binding = UserCardRowBinding.bind(holder.itemView)
        this.binding = binding
        val act = context as MainActivity
        act.animateAvd(binding.userCardEmptyImage, R.drawable.animated_party_popper, 1000)

        binding.userCard.setOnClickListener { openSheet(act) }
        binding.userCardEdit.setOnClickListener { openSheet(act) }
        if (binding.userCardContent.getTag(R.id.userCardContent) == null) {
            binding.userCardContent.setTag(R.id.userCardContent, true)
            binding.userCardContent.addOnAttachStateChangeListener(
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) = applyShine(binding)
                    override fun onViewDetachedFromWindow(view: View) = Unit
                }
            )
        }
        render(binding)
    }

    // showShimmer() alone is not enough: the layout starts out "shown", so it returns early
    // without starting anything. Showing and starting are two separate steps
    private fun applyShine(binding: UserCardRowBinding) =
        if (shine) {
            binding.userCardContent.showShimmer(true)
            binding.userCardContent.startShimmer()
        } else binding.userCardContent.hideShimmer()

    private fun openSheet(act: MainActivity) {
        if (act.supportFragmentManager.findFragmentByTag(SHEET_TAG) != null) return
        act.vibrate()
        UserBirthdayBottomSheet(act) {
            // Animate from whatever the card was showing to what was just saved or removed
            binding?.let { binding ->
                TransitionManager.beginDelayedTransition(
                    binding.userCard,
                    AutoTransition()
                        .setDuration(CARD_CHANGE_DURATION)
                        .setInterpolator(FastOutSlowInInterpolator())
                )
                render(binding)
            }
        }.show(act.supportFragmentManager, SHEET_TAG)
    }

    private fun render(binding: UserCardRowBinding) {
        val user = getUserBirthday(context)
        binding.userCardEmpty.isVisible = user == null
        binding.userCardFilled.isVisible = user != null
        // The whole card opens the sheet while empty, the pencil does it once filled
        binding.userCard.isClickable = user == null
        if (user == null) {
            shine = false
            applyShine(binding)
            return
        }

        val (name, birthday) = user
        binding.userCardName.text = name
        binding.userCardDate.text =
            birthday.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        loadUserImage(context)?.let { binding.userCardPhoto.setImageBitmap(it) }
            ?: binding.userCardPhoto.setImageResource(R.drawable.placeholder_birthday_image)

        val today = LocalDate.now()
        val next = nextUserBirthday(birthday, today)
        // On the day itself the card shines, if the user likes shiny things
        shine = next == today &&
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean("shimmer", false)
        applyShine(binding)
        binding.userCardCountdown.text =
            if (next == today) context.getString(R.string.user_birthday_greeting, name)
            else {
                val days = ChronoUnit.DAYS.between(today, next).toInt()
                val age = next.year - birthday.year
                formatDaysRemaining(days, context) + " · " +
                        context.resources.getQuantityString(R.plurals.years, age, age)
            }
    }

    private companion object {
        const val SHEET_TAG = "user_birthday_bottom_sheet"
    }
}
