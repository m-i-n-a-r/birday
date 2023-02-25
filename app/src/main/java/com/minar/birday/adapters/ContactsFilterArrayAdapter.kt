package com.minar.birday.adapters

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.minar.birday.model.ContactInfo
import java.util.*

class ContactsFilterArrayAdapter(
    context: Context,
    private val contacts: List<ContactInfo>,
    private val filterBy: (contactInfo: ContactInfo) -> (String),
) : ArrayAdapter<ContactInfo>(
    context,
    android.R.layout.simple_dropdown_item_1line,
    contacts.toMutableList()
) {

    init {
        clear()
        addAll(contacts)
    }

    override fun getFilter(): Filter = ContactsFilter()

    /**
     * Implementation of [Filter] similar to ArrayAdapter.ArrayFilter,
     * but it filters for a specified field.
     */
    private inner class ContactsFilter : Filter() {
        override fun performFiltering(prefix: CharSequence?): FilterResults {
            if (prefix == null || prefix.length < 2)
                return emptyList<ContactInfo>().toFilterResults()

            val prefixString = prefix.toString().lowercase(Locale.getDefault())
            val values = mutableListOf<ContactInfo>()
            values.addAll(contacts)

            return values.filter {
                val valueText: String = filterBy(it).lowercase(Locale.getDefault())

                // First match against the whole, non-split value
                if (valueText.startsWith(prefixString)) {
                    return@filter true
                } else {
                    val words = valueText.split(" ".toRegex())
                        .dropLastWhile(String::isEmpty)
                        .toTypedArray()
                    for (word in words) {
                        if (word.startsWith(prefixString)) {
                            return@filter true
                        }
                    }
                }

                false
            }.toFilterResults()
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            clear()
            addAll((results.values as List<*>).map { it as ContactInfo })
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        private fun List<ContactInfo>.toFilterResults(): FilterResults {
            val results = FilterResults()
            results.values = this@toFilterResults
            results.count = this@toFilterResults.size
            return results
        }
    }
}