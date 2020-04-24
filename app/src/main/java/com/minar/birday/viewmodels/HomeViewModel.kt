package com.minar.birday.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.minar.birday.persistence.Event
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventResult
import com.minar.birday.workers.EventWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)
    val allEvents: LiveData<List<EventResult>>
    val anyEvent: LiveData<List<EventResult>>
    private val eventDao: EventDao = EventDatabase.getBirdayDataBase(application)!!.eventDao()

    init {
        anyEvent = eventDao.getAnyEvent()
        allEvents = eventDao.getOrderedEvents()
    }

    // Launching new coroutines to insert the data in a non-blocking way

    fun insert(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.insertEvent(event)
    }

    fun delete(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.deleteEvent(event)
    }

    fun update(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.updateEvent(event)
    }

    // Check if there's a birthday today, using the hour range specified in shared preferences
    internal fun checkEvents() {
        //workManager.enqueue(PeriodicWorkRequest.from(EventWorker::class.java))
    }
}