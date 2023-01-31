package com.minar.birday.preferences.backup

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.facebook.shimmer.ShimmerFrameLayout
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.ContactsRepository
import kotlin.concurrent.thread


class ContactsImporter(context: Context, attrs: AttributeSet?) : Preference(context, attrs),
    View.OnClickListener {
    private val contactsRepository = ContactsRepository()

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val v = holder.itemView
        v.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        val shimmer = v as ShimmerFrameLayout

        // Disable the onclick and show the shimmer if the option is enabled
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val shimmerEnabled = sharedPrefs.getBoolean("shimmer", false)
        v.setOnClickListener(null)
        if (shimmerEnabled) {
            shimmer.startShimmer()
            shimmer.showShimmer(true)
        }
        act.vibrate()
        thread {
            importContacts(context)
            (context as MainActivity).runOnUiThread {
                if (shimmerEnabled) {
                    shimmer.stopShimmer()
                    shimmer.hideShimmer()
                }
                v.setOnClickListener(this)
            }
        }
    }

    // Import the contacts from device contacts (not necessarily Google)
    @SuppressLint("MissingPermission")
    fun importContacts(context: Context): Boolean {
        val act = context as MainActivity
        // Ask for contacts permission
        val permission = act.askContactsPermission(102)
        if (!permission) return false

        // Insert the remaining events in the db and update the recycler
        val events = contactsRepository.getEventsFromContacts(act.contentResolver)
        return if (events.isEmpty()) {
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_nothing_found))
            })
            true
        } else {
            act.mainViewModel.insertAll(events)
            context.runOnUiThread(Runnable {
                context.showSnackbar(context.getString(R.string.import_success))
            })
            true
        }
    }
}