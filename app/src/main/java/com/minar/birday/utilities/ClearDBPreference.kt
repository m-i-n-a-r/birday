package com.minar.birday.utilities

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.afollestad.materialdialogs.MaterialDialog
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.ClearDbRowBinding
import com.minar.birday.persistence.EventDatabase
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
        MaterialDialog(act).show {
            title(R.string.delete_db_dialog_title)
            message(R.string.delete_db_dialog_description)
            icon(R.drawable.ic_alert_24dp)
            cornerRadius(res = R.dimen.rounded_corners)

            negativeButton(R.string.cancel) {
                dismiss()
            }
            positiveButton {
                CoroutineScope(Dispatchers.IO).launch {
                    // Delete every saved data and send a snackbar
                    EventDatabase.getBirdayDatabase(context).clearAllTables()
                }
                act.showSnackbar(
                    context.getString(R.string.app_intro_done_button).lowercase()
                        .smartFixName(forceCapitalize = true)
                )
                dismiss()
            }
        }
    }

}
