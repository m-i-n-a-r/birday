package com.minar.birday.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.widget.AppCompatImageView

/**
 * An image view that owns a checked state, so the animated-selector icons of the navbar keep
 * working outside of BottomNavigationView: they pick their frames from android:state_checked, which
 * a plain ImageView never reports.
 */
class CheckableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), Checkable {

    private var checked = false

    override fun isChecked() = checked

    override fun setChecked(checked: Boolean) {
        if (this.checked == checked) return
        this.checked = checked
        refreshDrawableState()
    }

    override fun toggle() = setChecked(!checked)

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val state = super.onCreateDrawableState(extraSpace + 1)
        if (checked) mergeDrawableStates(state, CHECKED_STATE_SET)
        return state
    }

    private companion object {
        val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }
}
