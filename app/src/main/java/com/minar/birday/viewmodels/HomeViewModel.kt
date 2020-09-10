package com.minar.birday.viewmodels

import android.app.Application
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.minar.birday.model.Event
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.model.EventResult
import com.minar.birday.workers.EventWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)
    private val sp = PreferenceManager.getDefaultSharedPreferences(application)
    val allEvents: LiveData<List<EventResult>>
    val searchStringLiveData = MutableLiveData<String>()
    private val eventDao: EventDao = EventDatabase.getBirdayDatabase(application)!!.eventDao()

    init {
        searchStringLiveData.value = ""
        allEvents = Transformations.switchMap(searchStringLiveData) { string ->
            eventDao.getOrderedEventsByName(string)
        }
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
    fun checkEvents() {
        val workHour = sp.getString("notification_hour", "8")!!.toInt()
        // Cancel every previous scheduled work
        workManager.cancelAllWork()
        workManager.pruneWork()
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        // Set Execution at the time specified + 15 seconds to avoid midnight problems
        dueDate.set(Calendar.HOUR_OF_DAY, workHour)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 15)
        if (dueDate.before(currentDate)) dueDate.add(Calendar.HOUR_OF_DAY, 24)
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val dailyWorkRequest = OneTimeWorkRequestBuilder<EventWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueue(dailyWorkRequest)
    }

    // Update the name searched in the search bar
    fun searchNameChanged(name: String) {
        searchStringLiveData.value = name
    }
}