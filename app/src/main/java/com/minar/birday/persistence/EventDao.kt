package com.minar.birday.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.minar.birday.model.Event
import com.minar.birday.model.EventResult


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

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event ORDER BY nextDate, originalDate")
    fun getOrderedEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event ORDER BY nextDate, originalDate")
    fun getOrderedEventsStatic(): List<EventResult>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE name || ' ' || surname LIKE '%' || :searchString || '%' ORDER BY nextDate, originalDate")
    fun getOrderedEventsByName(searchString: String): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE type = :selectedType ORDER BY nextDate, originalDate")
    fun getOrderedEventsByType(selectedType: String): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE nextDate <> (SELECT CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedEventsExceptNext(): LiveData<List<EventResult>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE strftime('%Y', nextDate)-strftime('%Y', originalDate) = :age AND type = :type ORDER BY nextDate, originalDate;")
    fun getSpecialAgeEvents(age: Int, type: String): LiveData<List<Event>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM Event WHERE favorite = 1")
    fun getFavoriteEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE favorite = 1 ORDER BY nextDate, originalDate")
    fun getOrderedFavoriteEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE nextDate = (SELECT CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedNextEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE nextDate = (SELECT CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedNextEventsStatic(): List<EventResult>

    // Checkpoint functionality, not yet supported in room but useful to avoid closing the db during the backup creation
    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}