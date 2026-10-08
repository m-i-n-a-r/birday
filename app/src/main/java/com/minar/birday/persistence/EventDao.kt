package com.minar.birday.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult
import java.time.LocalDate


// The next time an event comes back. The Gregorian one is computed here, from the date alone; an
// alternative calendar moves it around the year in ways SQL can't follow, so that one is computed in
// Kotlin and stored. It is only used while it is set and not yet past, so with the alternative
// calendars turned off, or before a refresh, this is exactly the Gregorian date
private const val GREGORIAN_NEXT_DATE =
    "CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END"
private const val NEXT_DATE =
    "(CASE WHEN nextDateOverride >= date('now', 'localtime') THEN nextDateOverride ELSE $GREGORIAN_NEXT_DATE END)"

@Dao
interface EventDao {
    // Replace on conflict. This means that contacts will have priority over Birday data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEventReplace(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllEventReplace(events: List<Event>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateEventReplace(event: Event)

    // Ignore on conflict. This means that Birday will have priority over contacts data
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEventIgnore(event: Event)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllEventIgnore(events: List<Event>)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun updateEventIgnore(event: Event)

    @Delete
    fun deleteEvent(event: Event)

    @Delete
    fun deleteAllEvent(event: List<Event>)

    @Query("SELECT * FROM Event")
    fun getEvents(): LiveData<List<Event>>

    @Query("SELECT COUNT(id) FROM Event")
    fun getEventsCount(): LiveData<Int>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event ORDER BY nextDate, originalDate")
    fun getOrderedEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event ORDER BY nextDate, originalDate")
    fun getOrderedEventsStatic(): List<EventResult>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event WHERE name || ' ' || surname LIKE '%' || :searchString || '%' ORDER BY nextDate, originalDate")
    fun getOrderedEventsByName(searchString: String): LiveData<List<EventResult>>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event WHERE type = :selectedType ORDER BY nextDate, originalDate")
    fun getOrderedEventsByType(selectedType: String): LiveData<List<EventResult>>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event WHERE nextDate <> (SELECT $NEXT_DATE AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedEventsExceptNext(): LiveData<List<EventResult>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event WHERE strftime('%Y', nextDate)-strftime('%Y', originalDate) = :age AND type = :type ORDER BY nextDate, originalDate;")
    fun getSpecialAgeEvents(age: Int, type: String): LiveData<List<Event>>

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    // The events following an alternative calendar, whose next date has to be computed
    @Query("SELECT * FROM Event WHERE calendar IS NOT NULL")
    fun getAlternativeCalendarEvents(): List<Event>

    @Query("UPDATE Event SET nextDateOverride = :nextDate WHERE id = :id")
    fun setNextDateOverride(id: Int, nextDate: LocalDate?)

    @Query("UPDATE Event SET nextDateOverride = NULL WHERE nextDateOverride IS NOT NULL")
    fun clearNextDateOverrides()

    @Query("SELECT * FROM Event WHERE favorite = 1")
    fun getFavoriteEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event WHERE favorite = 1 ORDER BY nextDate, originalDate")
    fun getOrderedFavoriteEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event WHERE nextDate = (SELECT $NEXT_DATE AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedNextEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, $NEXT_DATE AS nextDate FROM Event WHERE nextDate = (SELECT $NEXT_DATE AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedNextEventsStatic(): List<EventResult>

    // Checkpoint functionality, not yet supported in room but useful to avoid closing the db during the backup creation
    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}