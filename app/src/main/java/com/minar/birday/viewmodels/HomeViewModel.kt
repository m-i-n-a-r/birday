package com.minar.birday.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.minar.birday.persistence.Event
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val allEvents: LiveData<List<EventResult>>
    private val eventDao: EventDao = EventDatabase.getBirdayDataBase(application)!!.eventDao()

    init {
        allEvents = eventDao.getOrderedEvents()
    }

    // Launching a new coroutine to insert the data in a non-blocking way
    fun insert(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.insertEvent(event)
    }
}