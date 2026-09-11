package com.minar.birday.utilities

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import com.google.android.material.R as MaterialR
import com.minar.birday.R

// The text size the compact widget uses when the user leaves the size on "Auto", read from the
// Material body style so the widget follows the same scale as the rest of the app.
// DisplayMetrics.scaledDensity is deprecated because font scaling is non linear since Android 14,
// so the sp/px ratio is derived through TypedValue instead.
fun Context.bodyMediumTextSizeSp(): Float {
    val pxPerSp = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, 1f, resources.displayMetrics
    )
    val attributes = obtainStyledAttributes(
        MaterialR.style.TextAppearance_Material3_BodyMedium,
        intArrayOf(android.R.attr.textSize)
    )
    val px = attributes.getDimension(0, DEFAULT_BODY_MEDIUM_SP * pxPerSp)
    attributes.recycle()
    return px / pxPerSp
}

private const val DEFAULT_BODY_MEDIUM_SP = 14f

private const val CASCADE_STAGGER = 35L
private const val CASCADE_DURATION = 240L
private const val CASCADE_OFFSET_DP = 18f

// Content rides in one after the other. Translation is optional: inside a MotionLayout the scene
// owns the positions, so there only the fade is safe
fun List<View>.animateCascade(translate: Boolean = true) {
    val density = firstOrNull()?.resources?.displayMetrics?.density ?: return
    forEachIndexed { index, view ->
        view.alpha = 0f
        if (translate) view.translationY = CASCADE_OFFSET_DP * density
        view.animate()
            .alpha(1f)
            .apply { if (translate) translationY(0f) }
            .setStartDelay(CASCADE_STAGGER * index)
            .setDuration(CASCADE_DURATION)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }
}

// Everything the container holds, minus the drag handle that has to stay where the finger left it
fun ViewGroup.animateChildrenCascade(translate: Boolean = true) =
    children.filter { it.id != R.id.dragHandle && it.isVisible }.toList().animateCascade(translate)

// Room under a scrolling view for the navbar, insets included. Needs clipToPadding off
fun View.addNavbarClearance() {
    val space = resources.getDimensionPixelSize(R.dimen.floating_navbar_space)
    val last = getTag(R.id.tag_navbar_clearance_bottom) as? Int ?: 0
    updatePadding(bottom = paddingBottom - last + space)
    setTag(R.id.tag_navbar_clearance_bottom, space)
    addInsetsByPadding(bottom = true)
}

fun View.addInsetsByPadding(
    top: Boolean = false,
    bottom: Boolean = false,
    left: Boolean = false,
    right: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val inset = Insets.max(
            insets.getInsets(WindowInsetsCompat.Type.systemBars()),
            insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        )
        if (top) {
            val lastTopPadding = view.getTag(R.id.view_add_insets_padding_top_tag) as? Int ?: 0
            val newTopPadding = inset.top
            view.setTag(R.id.view_add_insets_padding_top_tag, newTopPadding)
            view.updatePadding(top = view.paddingTop - lastTopPadding + newTopPadding)
        }
        if (bottom) {
            val lastBottomPadding = view.getTag(R.id.view_add_insets_padding_bottom_tag) as? Int ?: 0
            val newBottomPadding = inset.bottom
            view.setTag(R.id.view_add_insets_padding_bottom_tag, newBottomPadding)
            view.updatePadding(bottom = view.paddingBottom - lastBottomPadding + newBottomPadding)
        }
        if (left) {
            val lastLeftPadding = view.getTag(R.id.view_add_insets_padding_left_tag) as? Int ?: 0
            val newLeftPadding = inset.left
            view.setTag(R.id.view_add_insets_padding_left_tag, newLeftPadding)
            view.updatePadding(left = view.paddingLeft - lastLeftPadding + newLeftPadding)
        }
        if (right) {
            val lastRightPadding = view.getTag(R.id.view_add_insets_padding_right_tag) as? Int ?: 0
            val newRightPadding = inset.right
            view.setTag(R.id.view_add_insets_padding_right_tag, newRightPadding)
            view.updatePadding(right = view.paddingRight - lastRightPadding + newRightPadding)
        }
        return@setOnApplyWindowInsetsListener insets
    }
}

fun View.addInsetsByMargin(
    top: Boolean = false,
    bottom: Boolean = false,
    left: Boolean = false,
    right: Boolean = false,
    halveInsets: Boolean = false,
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val inset = Insets.max(
            insets.getInsets(WindowInsetsCompat.Type.systemBars()),
            insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        )
        if (top) {
            val lastTopMargin = view.getTag(R.id.view_add_insets_margin_top_tag) as? Int ?: 0
            val newTopMargin = if (halveInsets) inset.top / 2 else inset.top
            view.setTag(R.id.view_add_insets_margin_top_tag, newTopMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.topMargin = layoutParams.topMargin - lastTopMargin + newTopMargin
                view.layoutParams = layoutParams
            }
        }
        if (bottom) {
            val lastBottomMargin = view.getTag(R.id.view_add_insets_margin_bottom_tag) as? Int ?: 0
            val newBottomMargin = if (halveInsets) inset.bottom / 2 else inset.bottom
            view.setTag(R.id.view_add_insets_margin_bottom_tag, newBottomMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.bottomMargin = layoutParams.bottomMargin - lastBottomMargin + newBottomMargin
                view.layoutParams = layoutParams
            }
        }
        if (left) {
            val lastLeftMargin = view.getTag(R.id.view_add_insets_margin_left_tag) as? Int ?: 0
            val newLeftMargin = if (halveInsets) inset.left / 2 else inset.left
            view.setTag(R.id.view_add_insets_margin_left_tag, newLeftMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.leftMargin = layoutParams.leftMargin - lastLeftMargin + newLeftMargin
                view.layoutParams = layoutParams
            }
        }
        if (right) {
            val lastRightMargin = view.getTag(R.id.view_add_insets_margin_right_tag) as? Int ?: 0
            val newRightMargin = if (halveInsets) inset.right / 2 else inset.right
            view.setTag(R.id.view_add_insets_margin_right_tag, newRightMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.rightMargin = layoutParams.rightMargin - lastRightMargin + newRightMargin
                view.layoutParams = layoutParams
            }
        }
        return@setOnApplyWindowInsetsListener insets
    }
}