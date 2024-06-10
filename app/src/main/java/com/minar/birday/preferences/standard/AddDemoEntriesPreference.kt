package com.minar.birday.preferences.standard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.AddDemoEntriesRowBinding
import com.minar.birday.preferences.backup.CsvImporter
import com.minar.birday.utilities.getResourceUri
import com.minar.birday.utilities.smartFixName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


// A custom preference to add some demo entries to the DB
class AddDemoEntriesPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs),
    View.OnClickListener {
    private lateinit var binding: AddDemoEntriesRowBinding

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = AddDemoEntriesRowBinding.bind(holder.itemView)
        binding.root.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val act = context as MainActivity
        act.vibrate()

        MaterialAlertDialogBuilder(act)
            .setTitle(R.string.delete_db_dialog_title)
            .setIcon(R.drawable.ic_alert_24dp)
            .setMessage(R.string.add_demo_entries_dialog_description)
            .setPositiveButton(act.resources.getString(android.R.string.ok)) { dialog, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    // Insert a series of events from a csv in assets
                    val csvImporter = CsvImporter(act, null)
                    csvImporter.importEventsCsv(
                        act,
                        getResourceUri(R.raw.birday_demo_entries)
                    )
                }.invokeOnCompletion {
                    act.showSnackbar(
                        context.getString(R.string.app_intro_done_button).lowercase()
                            .smartFixName(forceCapitalize = true)
                    )
                    dialog.dismiss()
                }
            }
            .setNegativeButton(act.resources.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
