package com.minar.birday.preferences.standard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.ClearDbRowBinding
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.smartFixName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


// A custom preference to open the notification sound settings for Birday
@ExperimentalStdlibApi
class ClearDBPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
    View.OnClickListener {
    private lateinit var binding: ClearDbRowBinding

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = ClearDbRowBinding.bind(holder.itemView)
        binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()

        MaterialAlertDialogBuilder(act)
            .setTitle(R.string.delete_db_dialog_title)
            .setIcon(R.drawable.ic_alert_24dp)
            .setMessage(R.string.delete_db_dialog_description)
            .setPositiveButton(act.resources.getString(android.R.string.ok)) { dialog, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    // Delete every saved data and send a snackbar
                    EventDatabase.getBirdayDatabase(context).clearAllTables()
                }
                act.showSnackbar(
                    context.getString(R.string.app_intro_done_button).lowercase()
                        .smartFixName(forceCapitalize = true)
                )
                dialog.dismiss()
            }
            .setNegativeButton(act.resources.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
