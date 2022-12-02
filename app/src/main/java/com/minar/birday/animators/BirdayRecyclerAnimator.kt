package com.minar.birday.animators

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.util.Log
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator


// A custom animator to animate the items in recycler view
class BirdayRecyclerAnimator : SimpleItemAnimator() {

    // Never called
    override fun animateRemove(viewHolder: RecyclerView.ViewHolder): Boolean {
        viewHolder.itemView.animate()
            .alpha(0F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setStartDelay(200)
            .setDuration(300)
            .scaleY(0F)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchRemoveFinished(viewHolder)
                }
            })
            .start()
        Log.d("recycler_animation", "animate remove")
        return false
    }

    // Called when the items appear in the list (launch, fragment change, it was created)
    override fun animateAdd(viewHolder: RecyclerView.ViewHolder): Boolean {
        val height: Int = viewHolder.itemView.measuredHeight / 3
        val view = viewHolder.itemView
        view.translationY = height.toFloat()
        view.alpha = 0F
        view.scaleY = 1F
        view.animate()
            .translationY(0F)
            .alpha(1F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setDuration(400)
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

    // Called when an item is being moved to another position in the adapter
    override fun animateMove(
        viewHolder: RecyclerView.ViewHolder,
        fromX: Int, fromY: Int,
        toX: Int, toY: Int
    ): Boolean {
        val item = viewHolder.itemView
        item.y = fromY.toFloat()
        val verticalMovement = if (toY > fromY)
            (toY - fromY).toFloat() - (item.measuredHeight)
        else (fromY - toY).toFloat() - (item.measuredHeight)
        item.animate()
            .translationY(verticalMovement)
            .setDuration(300)
            .setInterpolator(FastOutSlowInInterpolator())
            .setStartDelay(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchMoveFinished(viewHolder)
                }
            })
            .start()
        Log.d("recycler_animation", "animate move")
        return false
    }

    // Called when an item changes its data
    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        fromLeft: Int, fromTop: Int,
        toLeft: Int, toTop: Int
    ): Boolean {
        newHolder.itemView.alpha = 0F
        val oldAnimation = oldHolder.itemView.animate()
            .alpha(0F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchChangeFinished(oldHolder, true)
                }
            })
        val newAnimation = newHolder.itemView.animate()
            .alpha(1F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchChangeFinished(newHolder, false)
                }
            })
        oldAnimation.start()
        newAnimation.start()
        Log.d("recycler_animation", "animate change")
        return false
    }

    // Called when an item is deleted from the adapter
    override fun animateDisappearance(
        viewHolder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo,
        postLayoutInfo: ItemHolderInfo?
    ): Boolean {
        viewHolder.itemView.animate()
            .alpha(0F)
            .setInterpolator(FastOutSlowInInterpolator())
            .setDuration(400)
            .setStartDelay(100)
            .scaleY(0F)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dispatchRemoveFinished(viewHolder)
                    viewHolder.itemView.alpha = 1F
                    viewHolder.itemView.scaleY = 1F
                }
            })
            .start()
        Log.d("recycler_animation", "animate disappearance")
        return false
    }

    override fun runPendingAnimations() {
        Log.d("recycler_animation", "pending animations")
    }

    override fun endAnimation(viewHolder: RecyclerView.ViewHolder) {
        Log.d("recycler_animation", "end animation")
        val item = viewHolder.itemView
        item.alpha = 1F
        item.translationY = 0F
        item.scaleY = 1F
    }

    override fun endAnimations() {
        Log.d("recycler_animation", "end animation no arg")
    }

    override fun isRunning(): Boolean {
        return false
    }


}