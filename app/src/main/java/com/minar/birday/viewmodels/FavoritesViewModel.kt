package com.minar.birday.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.minar.birday.model.Event
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.model.EventResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    val allFavoriteEvents: LiveData<List<EventResult>>
    val allEvents: LiveData<List<EventResult>>
    val anyFavoriteEvent: LiveData<List<EventResult>>
    private val eventDao: EventDao = EventDatabase.getBirdayDatabase(application)!!.eventDao()

    init {
        allEvents = eventDao.getOrderedEvents()
        anyFavoriteEvent = eventDao.getAnyFavoriteEvent()
        allFavoriteEvents = eventDao.getOrderedFavoriteEvents()
    }

    // Launching new coroutines to insert the data in a non-blocking way

    fun update(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.updateEvent(event)
    }
}