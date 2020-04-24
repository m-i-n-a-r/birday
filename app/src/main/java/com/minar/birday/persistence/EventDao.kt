package com.minar.birday.persistence

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: Event)

    @Update
    fun updateEvent(event: Event)

    @Delete
    fun deleteEvent(event: Event)

    @Query("SELECT * FROM Event")
    fun getEvents(): LiveData<List<Event>>

    @Query("SELECT * FROM Event WHERE name == :name")
    fun getEventByName(name: String): LiveData<List<Event>>

    @Query("SELECT *, CASE WHEN (strftime('%m', 'now') > strftime('%m', originalDate) OR (strftime('%m', 'now') = strftime('%m', originalDate) AND strftime('%d', 'now') > strftime('%d', originalDate))) THEN date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event ORDER BY nextDate, originalDate")
    fun getOrderedEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', 'now') > strftime('%m', originalDate) OR (strftime('%m', 'now') = strftime('%m', originalDate) AND strftime('%d', 'now') > strftime('%d', originalDate))) THEN date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE nextDate <> (SELECT CASE WHEN (strftime('%m', 'now') > strftime('%m', originalDate) OR (strftime('%m', 'now') = strftime('%m', originalDate) AND strftime('%d', 'now') > strftime('%d', originalDate))) THEN date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedEventsExceptNext(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', 'now') > strftime('%m', originalDate) OR (strftime('%m', 'now') = strftime('%m', originalDate) AND strftime('%d', 'now') > strftime('%d', originalDate))) THEN date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE strftime('%Y', nextDate)-strftime('%Y', originalDate) = :age AND type = :type ORDER BY nextDate, originalDate;")
    fun getSpecialAgeEvents(age: Int, type: String): LiveData<List<Event>>

    @Query("SELECT * FROM Event WHERE favorite = 1")
    fun getFavoriteEvents(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', 'now') > strftime('%m', originalDate) OR (strftime('%m', 'now') = strftime('%m', originalDate) AND strftime('%d', 'now') > strftime('%d', originalDate))) THEN date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE favorite = 1 ORDER BY nextDate, originalDate")
    fun getOrderedFavoriteEvents(): LiveData<List<EventResult>>

    @Query("SELECT COUNT(*) FROM Event")
    fun getCount(): LiveData<Int>

    @Query("SELECT * FROM Event LIMIT 1")
    fun getAnyEvent(): LiveData<List<EventResult>>

    @Query("SELECT * FROM Event WHERE favorite = 1 LIMIT 1")
    fun getAnyFavoriteEvent(): LiveData<List<EventResult>>

    @Query("SELECT *, CASE WHEN (strftime('%m', 'now') > strftime('%m', originalDate) OR (strftime('%m', 'now') = strftime('%m', originalDate) AND strftime('%d', 'now') > strftime('%d', originalDate))) THEN date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDate FROM Event WHERE nextDate = (SELECT CASE WHEN (strftime('%m', 'now') > strftime('%m', originalDate) OR (strftime('%m', 'now') = strftime('%m', originalDate) AND strftime('%d', 'now') > strftime('%d', originalDate))) THEN date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') ELSE date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) END AS nextDateFirst FROM Event ORDER BY nextDateFirst, originalDate limit 1) ORDER BY nextDate, originalDate")
    fun getOrderedNextEvents(): List<EventResult>
}