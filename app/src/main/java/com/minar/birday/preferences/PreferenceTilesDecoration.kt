package com.minar.birday.preferences

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isNotEmpty
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.minar.birday.R
import com.minar.birday.utilities.getThemeColor


// The settings as tonal tiles, one group per category: large corners where a group begins and
// ends, small ones in between, a thin gap between the tiles. The preferences stay what they are,
// only their rows are dressed. Anything outside a category, as the user card, is left alone
class PreferenceTilesDecoration(private val fragment: PreferenceFragmentCompat) :
    RecyclerView.ItemDecoration() {
    private val sideMargin =
        fragment.resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
    private val gap = fragment.resources.getDimensionPixelSize(R.dimen.preference_tile_gap)
    private val groupEnd =
        fragment.resources.getDimensionPixelSize(R.dimen.preference_tile_group_end)
    private val radius = fragment.resources.getDimension(R.dimen.preference_tile_radius)
    private val innerRadius =
        fragment.resources.getDimension(R.dimen.preference_tile_radius_inner)

    // The rows of the list in their order: the visible preferences, each category followed by
    // its own visible ones, as the preference adapter lays them out
    private fun visiblePreferences(): List<Preference> {
        val preferences = mutableListOf<Preference>()
        fun addVisible(group: PreferenceGroup) {
            for (index in 0 until group.preferenceCount) {
                val preference = group.getPreference(index)
                if (!preference.isVisible) continue
                preferences.add(preference)
                if (preference is PreferenceGroup) addVisible(preference)
            }
        }
        fragment.preferenceScreen?.let { addVisible(it) }
        return preferences
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val preferences = visiblePreferences()
        val position = parent.getChildAdapterPosition(view)
        val preference = preferences.getOrNull(position) ?: return
        val category = preference.parent as? PreferenceCategory ?: return
        val first = preferences.getOrNull(position - 1)?.parent != category
        val last = preferences.getOrNull(position + 1)?.parent != category
        outRect.set(sideMargin, 0, sideMargin, if (last) groupEnd else gap)

        // The offsets are asked again after every bind, and every bind gives the row back its
        // original background: this is the moment to replace it. A shimmering row is clickable
        // inside, so the tile goes there, or its rectangular ripple would cross the corners
        val tile = tileBackground(view.context, first, last)
        if (view is ShimmerFrameLayout && view.isNotEmpty()) {
            view.background = null
            (view as ViewGroup).getChildAt(0).background = tile
        } else view.background = tile
    }

    // The tonal color of the cards, and the ripple of a row within the tile
    private fun tileBackground(context: Context, first: Boolean, last: Boolean): Drawable {
        val top = if (first) radius else innerRadius
        val bottom = if (last) radius else innerRadius
        val shape = MaterialShapeDrawable(
            ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(top)
                .setTopRightCornerSize(top)
                .setBottomLeftCornerSize(bottom)
                .setBottomRightCornerSize(bottom)
                .build()
        )
        shape.fillColor = ColorStateList.valueOf(getThemeColor(R.attr.colorTonalCard, context))
        val ripple = getThemeColor(android.R.attr.colorControlHighlight, context)
        return RippleDrawable(ColorStateList.valueOf(ripple), shape, null)
    }
}
