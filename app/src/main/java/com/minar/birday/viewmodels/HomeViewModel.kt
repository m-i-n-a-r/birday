package com.minar.birday.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.minar.birday.persistence.Event
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.persistence.EventResult
import com.minar.birday.workers.EventWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)
    private val sp = PreferenceManager.getDefaultSharedPreferences(application)
    private val workHour = sp.getString("notification_hour", "8")!!.toInt()
    val allEvents: LiveData<List<EventResult>>
    val anyEvent: LiveData<List<EventResult>>
    private val eventDao: EventDao = EventDatabase.getBirdayDataBase(application)!!.eventDao()

    init {
        anyEvent = eventDao.getAnyEvent()
        allEvents = eventDao.getOrderedEvents()
        checkEvents()
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
    private fun checkEvents() {
        // Cancel every previous scheduled work
        workManager.pruneWork()
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        // Set Execution at the time specified
        dueDate.set(Calendar.HOUR_OF_DAY, workHour)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        if (dueDate.before(currentDate)) dueDate.add(Calendar.HOUR_OF_DAY, 24)
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val dailyWorkRequest = OneTimeWorkRequestBuilder<EventWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MINUTES)
            .build()
        workManager.enqueue(dailyWorkRequest)
    }
}