package com.minar.birday.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import com.minar.birday.R
import kotlin.math.max

// Children side by side, wrapping to a new line when they don't fit, every line centered.
// A ConstraintLayout Flow does the same through the linear solver, which inside a MotionLayout is
// measured twice and costs a visible frame. This is one pass over the children and nothing else
class CenteredFlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {
    private var horizontalGap = 0
    private var verticalGap = 0

    init {
        context.obtainStyledAttributes(attrs, R.styleable.CenteredFlowLayout).apply {
            horizontalGap = getDimensionPixelSize(R.styleable.CenteredFlowLayout_centeredFlowHorizontalGap, 0)
            verticalGap = getDimensionPixelSize(R.styleable.CenteredFlowLayout_centeredFlowVerticalGap, 0)
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        // A child is never wider than the whole line, so a long value wraps inside its own view
        val childWidthSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        var lineWidth = 0
        var lineHeight = 0
        var totalHeight = 0
        var widest = 0
        for (child in visibleChildren()) {
            child.measure(childWidthSpec, childHeightSpec)
            val needed = if (lineWidth == 0) child.measuredWidth else lineWidth + horizontalGap + child.measuredWidth
            if (lineWidth != 0 && needed > maxWidth) {
                totalHeight += lineHeight + verticalGap
                widest = max(widest, lineWidth)
                lineWidth = child.measuredWidth
                lineHeight = child.measuredHeight
            } else {
                lineWidth = needed
                lineHeight = max(lineHeight, child.measuredHeight)
            }
        }
        totalHeight += lineHeight
        widest = max(widest, lineWidth)
        setMeasuredDimension(
            resolveSize(widest + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(totalHeight + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxWidth = r - l - paddingLeft - paddingRight
        var top = paddingTop
        val line = mutableListOf<View>()
        var lineWidth = 0

        fun placeLine() {
            // Center the line, every child vertically centered in it too
            var left = paddingLeft + (maxWidth - lineWidth) / 2
            val lineHeight = line.maxOfOrNull { it.measuredHeight } ?: 0
            for (child in line) {
                val childTop = top + (lineHeight - child.measuredHeight) / 2
                child.layout(left, childTop, left + child.measuredWidth, childTop + child.measuredHeight)
                left += child.measuredWidth + horizontalGap
            }
            top += lineHeight + verticalGap
            line.clear()
            lineWidth = 0
        }

        for (child in visibleChildren()) {
            val needed = if (line.isEmpty()) child.measuredWidth else lineWidth + horizontalGap + child.measuredWidth
            if (line.isNotEmpty() && needed > maxWidth) placeLine()
            lineWidth = if (line.isEmpty()) child.measuredWidth else lineWidth + horizontalGap + child.measuredWidth
            line.add(child)
        }
        if (line.isNotEmpty()) placeLine()
    }

    private fun visibleChildren() = (0 until childCount).map { getChildAt(it) }.filterNot { it.isGone }
}
