package com.minar.birday.animators

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.util.Log
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator


// A custom animator to animate the items in recycler view
// Initial appearance: animate add
// When deleting: animate disappearance on item deleted, animate move on others
// When adding: animate add on item added, animate move on others
class RecyclerAnimator : SimpleItemAnimator() {

    override fun animateRemove(viewHolder: RecyclerView.ViewHolder): Boolean {
        viewHolder.itemView.animate()
            .scaleY(0F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchAddFinished(viewHolder)
                }
            })
            .start()
        Log.d("recycler_animation", "animate remove")
        return false
    }

    override fun animateAdd(viewHolder: RecyclerView.ViewHolder): Boolean {
        val height: Int = viewHolder.itemView.measuredHeight / 3
        viewHolder.itemView.translationY = height.toFloat()
        viewHolder.itemView.animate()
            .translationY(0F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setDuration(500)
            .setStartDelay(viewHolder.bindingAdapterPosition * 50L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchAddFinished(viewHolder)
                }
            })
            .start()
        Log.d("recycler_animation", "animate add")
        return false
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder?,
        fromX: Int, fromY: Int,
        toX: Int, toY: Int
    ): Boolean {
        Log.d("recycler_animation", "animate move")
        return false
    }

    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder?,
        newHolder: RecyclerView.ViewHolder?,
        fromLeft: Int, fromTop: Int,
        toLeft: Int, toTop: Int
    ): Boolean {
        Log.d("recycler_animation", "animate change")
        return false
    }

    override fun animateDisappearance(
        viewHolder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo,
        postLayoutInfo: ItemHolderInfo?
    ): Boolean {
        viewHolder.itemView.animate()
            .scaleY(0F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchAddFinished(viewHolder)
                }
            })
            .start()
        Log.d("recycler_animation", "animate disappearance")
        return false
    }

    override fun runPendingAnimations() {}

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        Log.d("recycler_animation", "end animation")
    }

    override fun endAnimations() {}

    override fun isRunning(): Boolean {
        return false
    }


}