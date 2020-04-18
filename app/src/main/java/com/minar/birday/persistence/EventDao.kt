package com.minar.birday.persistence

import androidx.room.*
import java.time.LocalDate

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: Event)

    @Update
    fun updateEvent(event: Event)

    @Delete
    fun deleteEvent(event: Event)

    @Query("SELECT * FROM Event WHERE name == :name")
    fun getEventByName(name: String): List<Event>

    @Query("SELECT * FROM Event")
    fun getEvents(): List<Event>

    @Query("SELECT *, CASE when (strftime('%m', 'now') > strftime('%m', originalDate) or (strftime('%m', 'now') = strftime('%m', originalDate) and strftime('%d', 'now') > strftime('%d', originalDate))) then date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate), '+1 year') else date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) end as nextDate FROM Event order by nextDate, originalDate asc")
    fun getEventsOrdered(): List<EventResult>

    @Query("SELECT *, CASE when (strftime('%m', 'now') > strftime('%m', originalDate) or (strftime('%m', 'now') = strftime('%m', originalDate) and strftime('%d', 'now') > strftime('%d', originalDate))) then date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate),'+1 year') else date(strftime('%Y', 'now') || '-' || strftime('%m', originalDate) || '-' || strftime('%d', originalDate)) end as nextDate FROM Event WHERE nextDate <> :date order by nextDate, originalDate")
    fun getOrderedEventsExceptNext(date: LocalDate): List<EventResult>

    // TODO select the type
    @Query("SELECT * from Event where strftime('%Y', 'now') - strftime('%Y', originalDate) = :age and strftime('%m', 'now') < strftime('%m', originalDate) or (strftime('%m', 'now') = strftime('%m', originalDate) and strftime('%d', 'now') <= strftime('%d', originalDate))")
    fun getSpecialAgeEvents(age: Int): List<Event>
}