package com.minar.birday.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery


@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEvent(event: Event)

    @Update
    fun updateEvent(event: Event)

    @Delete
    fun deleteEvent(event: Event)

    @Query("SELECT * FROM Event")
    fun getEvents(): LiveData<List<Event>>

    @Query("SELECT * FROM Event WHERE name == :name")
    fun getEventByName(name: String): LiveData<List<Event>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event ORDER BY nextDate, originalDate")
    fun getOrderedEvents(): LiveData<List<EventResult>>

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

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM Event LIMIT 1")
    fun getAnyEvent(): LiveData<List<EventResult>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM Event WHERE favorite = 1 LIMIT 1")
    fun getAnyFavoriteEvent(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE nextDate = (SELECT CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedNextEvents(): List<EventResult>

    @Query("SELECT *, CASE WHEN (strftime('%m', datetime('now', 'localtime')) > strftime('%m', originalDate) OR (strftime('%m', datetime('now', 'localtime')) = strftime('%m', originalDate) AND strftime('%d', datetime('now', 'localtime')) > strftime('%d', originalDate))) THEN date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', datetime('now', 'localtime')) || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event ORDER BY nextDate, originalDate")
    fun getOrderedAllEvents(): List<EventResult>

    // Checkpoint functionality, not yet supported in room but useful to avoid closing the db during the backup creation
    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery?): Int
}