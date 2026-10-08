package com.minar.birday.preferences.standard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.AddDemoEntriesRowBinding
import com.minar.birday.preferences.backup.CsvImporter
import com.minar.birday.utilities.getResourceUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
                dialog.dismiss()
                // The demo set is thousands of rows, so it is parsed off the main thread and then
                // inserted whole. Deliberately not importEventsCsv: that one ends in the picker,
                // which would have to render a preview line per event and offer them all as
                // checkboxes. Picking among thousands of famous birthdays is not what this is for
                act.lifecycleScope.launch(Dispatchers.IO) {
                    val events = CsvImporter(act, null)
                        .parseEventsCsv(act, getResourceUri(R.raw.birday_demo_entries))
                    withContext(Dispatchers.Main) {
                        when {
                            events == null ->
                                act.showSnackbar(act.getString(R.string.birday_import_failure))

                            events.isEmpty() ->
                                act.showSnackbar(act.getString(R.string.import_nothing_found))

                            else -> {
                                act.mainViewModel.insertAll(events)
                                act.showSnackbar(act.getString(R.string.import_success))
                            }
                        }
                    }
                }
            }
            .setNegativeButton(act.resources.getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

}
