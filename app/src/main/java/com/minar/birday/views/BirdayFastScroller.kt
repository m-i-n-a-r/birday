package com.minar.birday.views

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.core.graphics.withClip
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import com.minar.birday.R
import com.minar.birday.utilities.getThemeColor
import kotlin.math.max
import kotlin.math.roundToInt


// Idle time before the scroller fades away, and the duration of the fade
private const val HIDE_DELAY = 1500L
private const val FADE_DURATION = 200L
// The bubble passing from a section to the next
private const val SECTION_DURATION = 180L

/**
 * The fast scroll of the event lists, drawn by the list itself: there are no views, so nothing is
 * laid out again while dragging. Unlike the one of RecyclerView it follows the padding of the
 * list, so the track starts below the card, and it can show a bubble with the section at the top.
 * @param sectionText The text of the bubble for the given adapter position, no bubble if null
 */
class BirdayFastScroller(
    private val recycler: RecyclerView,
    private val sectionText: ((position: Int) -> CharSequence)? = null,
) : RecyclerView.ItemDecoration(), RecyclerView.OnItemTouchListener {
    private val context = recycler.context
    private val thumbWidth = dimen(R.dimen.fast_scroll_width)
    private val thumbHeight = dimen(R.dimen.fast_scroll_thumb_height)
    private val edgeInset = dimen(R.dimen.fast_scroll_edge_inset)
    private val touchWidth = dimen(R.dimen.fast_scroll_touch_width)
    private val bubbleSize = dimen(R.dimen.fast_scroll_popup_size)
    private val bubbleMargin = dimen(R.dimen.fast_scroll_popup_margin)
    private val bubblePadding = dimen(R.dimen.fast_scroll_popup_padding)

    private val trackColor = getThemeColor(R.attr.colorSurfaceVariant, context)
    private val thumbColor = getThemeColor(R.attr.colorPrimary, context)
    private val bubbleColor = getThemeColor(R.attr.colorPrimaryContainer, context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(
        TextView(context).apply {
            setTextAppearance(MaterialR.style.TextAppearance_Material3_TitleLarge)
        }.paint
    ).apply {
        color = getThemeColor(R.attr.colorOnPrimaryContainer, context)
        textAlign = Paint.Align.CENTER
    }
    private val textColor = textPaint.color
    private val rect = RectF()

    private var visibility = 0f
    private var fade: ValueAnimator? = null
    private var fadeTarget = 0f
    private var dragging = false
    private var grabOffset = 0f
    // While dragging the thumb stays under the finger, whatever the estimate of the list says
    private var dragThumbTop = 0f
    // The bubble: what it says, what it said, and how far the change between the two has gone
    private var bubbleText = ""
    private var previousBubbleText = ""
    private var bubbleWidthFrom = 0f
    private var bubbleWidthTo = 0f
    private var sectionProgress = 1f
    private var sectionAnimator: ValueAnimator? = null
    private val hide = Runnable { if (!dragging) fadeTo(0f) }

    init {
        recycler.addItemDecoration(this)
        recycler.addOnItemTouchListener(this)
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0 && isWorthIt()) show()
            }
        })
    }

    private fun dimen(@DimenRes id: Int) = context.resources.getDimension(id)

    private val isRtl get() = recycler.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private val trackTop get() = recycler.paddingTop.toFloat()
    private val trackBottom get() = (recycler.height - recycler.paddingBottom).toFloat()
    private val thumbTravel get() = trackBottom - trackTop - thumbHeight
    private val scrollRange
        get() = recycler.computeVerticalScrollRange() - recycler.computeVerticalScrollExtent()

    // Only for lists at least two screens long, with room for the track
    private fun isWorthIt() = scrollRange > recycler.height && thumbTravel > thumbHeight

    private fun thumbLeft() =
        if (isRtl) edgeInset else recycler.width - edgeInset - thumbWidth

    private fun thumbTop(): Float {
        val fraction = recycler.computeVerticalScrollOffset().toFloat() / scrollRange
        return trackTop + fraction.coerceIn(0f, 1f) * thumbTravel
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (visibility == 0f || !isWorthIt()) return
        val left = thumbLeft()
        val radius = thumbWidth / 2
        rect.set(left, trackTop, left + thumbWidth, trackBottom)
        canvas.drawRoundRect(rect, radius, radius, paint.faded(trackColor))
        val thumbTop = if (dragging) dragThumbTop else thumbTop()
        rect.set(left, thumbTop, left + thumbWidth, thumbTop + thumbHeight)
        canvas.drawRoundRect(rect, radius, radius, paint.faded(thumbColor))
        if (dragging) drawBubble(canvas, left, thumbTop + thumbHeight / 2)
    }

    // The section of the row at the top of the list, beside the thumb. One bubble all along: a new
    // section fades in over the old one while the bubble stretches or shrinks to fit it
    private fun drawBubble(canvas: Canvas, thumbLeft: Float, thumbCenter: Float) {
        val sectionText = sectionText ?: return
        // Between two rows there's no child: the bubble keeps what it says
        val child = recycler.findChildViewUnder(recycler.width / 2f, trackTop)
        val position = child?.let { recycler.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
        if (position != RecyclerView.NO_POSITION) {
            val text = sectionText(position).toString()
            if (text.isNotEmpty() && text != bubbleText) changeSection(text)
        }
        if (bubbleText.isEmpty()) return

        val width = bubbleWidthFrom + (bubbleWidthTo - bubbleWidthFrom) * sectionProgress
        val centerY = thumbCenter.coerceIn(trackTop + bubbleSize / 2, trackBottom - bubbleSize / 2)
        val start =
            if (isRtl) thumbLeft + thumbWidth + bubbleMargin
            else thumbLeft - bubbleMargin - width
        rect.set(start, centerY - bubbleSize / 2, start + width, centerY + bubbleSize / 2)
        canvas.drawRoundRect(rect, bubbleSize / 2, bubbleSize / 2, paint.faded(bubbleColor))
        val baseline = centerY - (textPaint.ascent() + textPaint.descent()) / 2
        canvas.withClip(rect) {
            if (sectionProgress < 1f)
                drawBubbleText(this, previousBubbleText, baseline, 1f - sectionProgress)
            drawBubbleText(this, bubbleText, baseline, sectionProgress)
        }
    }

    private fun drawBubbleText(canvas: Canvas, text: String, baseline: Float, opacity: Float) {
        if (text.isEmpty() || opacity <= 0f) return
        textPaint.color = textColor
        textPaint.alpha = (Color.alpha(textColor) * visibility * opacity).roundToInt()
        canvas.drawText(text, rect.centerX(), baseline, textPaint)
    }

    private fun bubbleWidthFor(text: String) =
        max(bubbleSize, textPaint.measureText(text) + 2 * bubblePadding)

    // The first section shows up as it is, the next ones take over the bubble
    private fun changeSection(text: String) {
        sectionAnimator?.cancel()
        val currentWidth = bubbleWidthFrom + (bubbleWidthTo - bubbleWidthFrom) * sectionProgress
        previousBubbleText = bubbleText
        bubbleText = text
        if (previousBubbleText.isEmpty()) {
            bubbleWidthFrom = bubbleWidthFor(text)
            bubbleWidthTo = bubbleWidthFrom
            sectionProgress = 1f
            return
        }
        bubbleWidthFrom = currentWidth
        bubbleWidthTo = bubbleWidthFor(text)
        sectionProgress = 0f
        sectionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SECTION_DURATION
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                sectionProgress = it.animatedValue as Float
                recycler.invalidate()
            }
            start()
        }
    }

    private fun Paint.faded(base: Int): Paint {
        color = base
        alpha = (Color.alpha(base) * visibility).roundToInt()
        return this
    }

    override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return dragging
        if (visibility == 0f || !isWorthIt() || !isOnTrack(event.x, event.y)) return false
        startDrag(event.y)
        return true
    }

    override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> scrollToThumb(event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                // The next drag starts from a fresh bubble
                sectionAnimator?.cancel()
                bubbleText = ""
                sectionProgress = 1f
                show()
                recycler.invalidate()
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    private fun isOnTrack(x: Float, y: Float): Boolean {
        val onEdge = if (isRtl) x <= touchWidth else x >= recycler.width - touchWidth
        return onEdge && y >= trackTop && y <= trackBottom
    }

    // Grabbing the thumb keeps it under the finger, touching the track brings it there
    private fun startDrag(y: Float) {
        dragging = true
        recycler.parent?.requestDisallowInterceptTouchEvent(true)
        val thumbTop = thumbTop()
        grabOffset = if (y in thumbTop..thumbTop + thumbHeight) y - thumbTop else thumbHeight / 2
        show()
        scrollToThumb(y)
        recycler.invalidate()
    }

    private fun scrollToThumb(y: Float) {
        val fraction = ((y - grabOffset - trackTop) / thumbTravel).coerceIn(0f, 1f)
        dragThumbTop = trackTop + fraction * thumbTravel
        recycler.invalidate()
        val delta = (fraction * scrollRange).roundToInt() - recycler.computeVerticalScrollOffset()
        if (delta != 0) recycler.scrollBy(0, delta)
    }

    // Visible while scrolling, gone a moment after
    private fun show() {
        recycler.removeCallbacks(hide)
        fadeTo(1f)
        if (!dragging) recycler.postDelayed(hide, HIDE_DELAY)
    }

    private fun fadeTo(target: Float) {
        if (fadeTarget == target && (fade?.isRunning == true || visibility == target)) return
        fadeTarget = target
        fade?.cancel()
        fade = ValueAnimator.ofFloat(visibility, target).apply {
            duration = FADE_DURATION
            addUpdateListener {
                visibility = it.animatedValue as Float
                recycler.invalidate()
            }
            start()
        }
    }
}
