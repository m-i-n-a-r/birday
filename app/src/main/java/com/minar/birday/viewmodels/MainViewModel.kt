package com.minar.birday.viewmodels

import android.app.Application
import android.content.Context
import android.text.SpannableStringBuilder
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.StatsGenerator
import com.minar.birday.workers.EventWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val workManager = WorkManager.getInstance(application)
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application)
    val allEvents: LiveData<List<EventResult>>
    val allEventsUnfiltered: LiveData<List<EventResult>>
    private val eventsCount: LiveData<Int> // Unused since it's null sometimes
    val searchString = MutableLiveData<String>()
    val selectedType = MutableLiveData<String>()
    private val searchValues = MediatorLiveData<Pair<String?, String?>>()
    var fullStats: SpannableStringBuilder? = null
    private val eventDao: EventDao = EventDatabase.getBirdayDatabase(application).eventDao()
    var confettiDone: Boolean = false

    init {
        searchString.value = ""
        selectedType.value = ""
        // The Pair values are nullable as getting "liveData.value" can be null
        searchValues.apply {
            addSource(searchString) { value = it to selectedType.value }
            addSource(selectedType) { value = searchString.value to it }
        }

        // All the events, unfiltered
        allEventsUnfiltered = eventDao.getOrderedEvents()
        // All the events, filtered by search string and type
        allEvents = searchValues.switchMap { pair ->
            val searchString = pair.first
            val selectedType = pair.second
            if (!searchString.isNullOrBlank())
                eventDao.getOrderedEventsByName(searchString)
            else if (!selectedType.isNullOrBlank())
                eventDao.getOrderedEventsByType(selectedType)
            else eventDao.getOrderedEventsByName("")
        }
        eventsCount = eventDao.getEventsCount()
        scheduleNextCheck()
    }

    // Launching new coroutines to insert the data in a non-blocking way

    fun getStats(events: List<EventResult>, context: Context, astrologyDisabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            val generator = StatsGenerator(events, context, astrologyDisabled)
            fullStats = generator.generateFullStats()
        }

    fun getFavorites(): LiveData<List<EventResult>> =
        eventDao.getOrderedFavoriteEvents()

    fun insert(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.insertEvent(event)
    }

    fun insertAll(events: List<Event>) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.insertAllEvent(events)
    }

    fun delete(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.deleteEvent(event)
    }

    fun deleteAll(events: List<Event>) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.deleteAllEvent(events)
    }

    fun update(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.updateEvent(event)
    }

    // Schedule the next work for the specified hour, nothing will happen if there's no event
    fun scheduleNextCheck() {
        val workHour = sharedPrefs.getString("notification_hour", "8")!!.toInt()
        val workMinute = sharedPrefs.getString("notification_minute", "0")!!.toInt()
        // Cancel every previous scheduled work
        workManager.cancelAllWork()
        workManager.pruneWork()
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        // Set Execution at the time specified + 15 seconds to avoid midnight problems
        dueDate.set(Calendar.HOUR_OF_DAY, workHour)
        dueDate.set(Calendar.MINUTE, workMinute)
        dueDate.set(Calendar.SECOND, 15)
        if (dueDate.before(currentDate)) dueDate.add(Calendar.HOUR_OF_DAY, 24)
        // Setup the work request using the difference between now and the next check as delay
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val dailyWorkRequest = OneTimeWorkRequestBuilder<EventWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()
        // Enqueue the request
        workManager.enqueue(dailyWorkRequest)
    }

    // Update the name searched in the search bar
    fun searchStringChanged(newSearchString: String) {
        if (searchString.value == newSearchString) return
        searchString.value = newSearchString
    }

    // Update the type selected in the search bar
    fun eventTypeChanged(newSelectedType: String?) {
        if (selectedType.value == newSelectedType) return
        selectedType.value = newSelectedType ?: ""
    }
}