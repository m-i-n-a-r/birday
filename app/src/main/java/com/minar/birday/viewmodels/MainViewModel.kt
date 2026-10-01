package com.minar.birday.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import com.minar.birday.model.Stat
import com.minar.birday.persistence.EventDao
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.StatsGenerator
import com.minar.birday.utilities.delayToNextCheck
import com.minar.birday.utilities.refreshCalendarDates
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
    private val refreshTrigger = MutableLiveData<Unit>(Unit)
    var fullStats = MutableLiveData<List<Stat>>()
    // The favorites card subtitle. It used to be built in the fragment, on the main thread, and the
    // first open of the tab paid the whole bill: generateRandomStat() keeps re-rolling until a stat
    // comes out non blank, and every roll walks the event list again
    var randomStat = MutableLiveData<String>()
    private val eventDao: EventDao = EventDatabase.getBirdayDatabase(application).eventDao()
    var confettiDone: Boolean = false
    // The user's own birthday is celebrated once per session, apart from the events
    var userBirthdayCelebrated: Boolean = false

    init {
        searchString.value = ""
        selectedType.value = ""
        refreshCalendars()
        // The Pair values are nullable as getting "liveData.value" can be null
        searchValues.apply {
            addSource(searchString) { value = it to selectedType.value }
            addSource(selectedType) { value = searchString.value to it }
            addSource(refreshTrigger) { value = searchString.value to selectedType.value }
        }

        // All the events, unfiltered
        allEventsUnfiltered = refreshTrigger.switchMap { eventDao.getOrderedEvents() }
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

    fun getStats(events: List<EventResult>, context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            val astrologyDisabled = sharedPrefs.getBoolean("disable_astrology", false)
            val generator = StatsGenerator(events, context, astrologyDisabled)
            fullStats.postValue(generator.generateFullStats())
        }

    // Rolled again every time the favorites come back on screen, so the card greets you with a
    // different stat each visit. It is a separate call from getStats because that one only runs
    // when the events change, while this is supposed to run on every return to the tab
    fun refreshRandomStat(events: List<EventResult>, context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            val astrologyDisabled = sharedPrefs.getBoolean("disable_astrology", false)
            randomStat.postValue(
                StatsGenerator(events, context, astrologyDisabled).generateRandomStat()
            )
        }

    fun getFavorites(): LiveData<List<EventResult>> =
        eventDao.getOrderedFavoriteEvents()

    fun insert(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        val replaceOnConflict = sharedPrefs.getBoolean("replace_on_conflict", true)
        if (replaceOnConflict)
            eventDao.insertEventReplace(event) else eventDao.insertEventIgnore(event)
        refreshCalendarDates(getApplication())
    }

    fun insertAll(events: List<Event>) = viewModelScope.launch(Dispatchers.IO) {
        val replaceOnConflict = sharedPrefs.getBoolean("replace_on_conflict", true)
        if (replaceOnConflict)
            eventDao.insertAllEventReplace(events) else eventDao.insertAllEventIgnore(events)
        refreshCalendarDates(getApplication())
    }

    fun delete(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.deleteEvent(event)
    }

    fun deleteAll(events: List<Event>) = viewModelScope.launch(Dispatchers.IO) {
        eventDao.deleteAllEvent(events)
    }

    fun update(event: Event) = viewModelScope.launch(Dispatchers.IO) {
        val replaceOnConflict = sharedPrefs.getBoolean("replace_on_conflict", true)
        if (replaceOnConflict)
            eventDao.updateEventReplace(event) else eventDao.updateEventIgnore(event)
        refreshCalendarDates(getApplication())
    }

    // The dates of the alternative calendars move day by day, and with the setting
    fun refreshCalendars() = viewModelScope.launch(Dispatchers.IO) {
        refreshCalendarDates(getApplication())
    }

    // Schedule the next work for the specified hour, nothing will happen if there's no event
    fun scheduleNextCheck() {
        val workHour = sharedPrefs.getString("notification_hour", "8")!!.toInt()
        val workMinute = sharedPrefs.getString("notification_minute", "0")!!.toInt()
        // Cancel every previous scheduled work
        workManager.cancelAllWork()
        workManager.pruneWork()
        // Setup the work request using the time until the next check as delay
        val dailyWorkRequest = OneTimeWorkRequestBuilder<EventWorker>()
            .setInitialDelay(delayToNextCheck(workHour, workMinute).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        // Enqueue the request
        workManager.enqueue(dailyWorkRequest)
    }

    fun refreshEvents() {
        refreshTrigger.value = Unit
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