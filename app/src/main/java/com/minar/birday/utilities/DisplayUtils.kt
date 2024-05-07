package com.minar.birday.utilities

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.minar.birday.R

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
            val lastBottomPadding =
                view.getTag(R.id.view_add_insets_padding_bottom_tag) as? Int ?: 0
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
    right: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val inset = Insets.max(
            insets.getInsets(WindowInsetsCompat.Type.systemBars()),
            insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        )
        if (top) {
            val lastTopMargin = view.getTag(R.id.view_add_insets_margin_top_tag) as? Int ?: 0
            val newTopMargin = inset.top
            view.setTag(R.id.view_add_insets_margin_top_tag, newTopMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.topMargin = layoutParams.topMargin - lastTopMargin + newTopMargin
                view.layoutParams = layoutParams
            }
        }
        if (bottom) {
            val lastBottomMargin = view.getTag(R.id.view_add_insets_margin_bottom_tag) as? Int ?: 0
            val newBottomMargin = inset.bottom
            view.setTag(R.id.view_add_insets_margin_bottom_tag, newBottomMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.bottomMargin = layoutParams.bottomMargin - lastBottomMargin + newBottomMargin
                view.layoutParams = layoutParams
            }
        }
        if (left) {
            val lastLeftMargin = view.getTag(R.id.view_add_insets_margin_left_tag) as? Int ?: 0
            val newLeftMargin = inset.left
            view.setTag(R.id.view_add_insets_margin_left_tag, newLeftMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.leftMargin = layoutParams.leftMargin - lastLeftMargin + newLeftMargin
                view.layoutParams = layoutParams
            }
        }
        if (right) {
            val lastRightMargin = view.getTag(R.id.view_add_insets_margin_right_tag) as? Int ?: 0
            val newRightMargin = inset.right
            view.setTag(R.id.view_add_insets_margin_right_tag, newRightMargin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { layoutParams ->
                layoutParams.rightMargin = layoutParams.rightMargin - lastRightMargin + newRightMargin
                view.layoutParams = layoutParams
            }
        }
        return@setOnApplyWindowInsetsListener insets
    }
}