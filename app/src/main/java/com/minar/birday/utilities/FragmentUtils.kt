package com.minar.birday.utilities

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

fun DialogFragment.showIfNotAdded(fragmentManager: FragmentManager, tag: String) {
    if (!isAdded) {
        show(fragmentManager, tag)
    }
}