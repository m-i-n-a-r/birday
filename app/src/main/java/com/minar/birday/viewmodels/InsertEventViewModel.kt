package com.minar.birday.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minar.birday.activities.MainActivity
import com.minar.birday.model.ContactInfo
import com.minar.birday.persistence.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalStdlibApi
class InsertEventViewModel(application: Application) : AndroidViewModel(application) {
    private val contactsRepository = ContactsRepository()

    private val _contactsList = mutableListOf<ContactInfo>()

    // Don't expose the Mutable version explicitly
    val contactsList: List<ContactInfo> = _contactsList

    @SuppressLint("MissingPermission")
    fun initContactsList(act: MainActivity) {
        if (act.askContactsPermission()) {

            val resolver = getApplication<Application>().contentResolver
            viewModelScope.launch {
                val contacts = withContext(Dispatchers.IO) {
                    contactsRepository.queryContacts(resolver)
                }
                _contactsList.clear()
                _contactsList.addAll(contacts)
            }
        }
    }
}