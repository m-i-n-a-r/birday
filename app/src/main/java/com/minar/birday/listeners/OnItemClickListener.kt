package com.minar.birday.listeners

import android.view.View

interface OnItemClickListener {
    fun onItemClick(position: Int, view: View?)
    fun onItemLongClick(position: Int, view: View?): Boolean
}