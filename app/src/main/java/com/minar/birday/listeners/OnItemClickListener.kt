package com.minar.birday.listeners

import android.view.View

interface OnItemClickListener {
    abstract fun onItemClick(position: Int, view: View?)

    abstract fun onItemLongClick(position: Int, view: View?): Boolean
}