package com.minar.birday.persistence

import androidx.room.*

@Dao
interface BirthdayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBirthday(gender: Birthday)

    @Update
    fun updateBirthday(gender: Birthday)

    @Delete
    fun deleteBirthday(gender: Birthday)

    @Query("SELECT * FROM Birthday WHERE name == :name")
    fun getBirthdayByName(name: String): List<Birthday>

    @Query("SELECT * FROM Birthday")
    fun getBirthdays(): List<Birthday>

    @Query("SELECT *, CASE WHEN (strftime('%m','now') > strftime('%m',date) or (strftime('%m','now') = strftime('%m',date) and strftime('%d','now') > strftime('%d',date)))  then DATE(date, + (strftime('%Y','now')-strftime('%Y',date)+1  year) else DATE(date, + (strftime('%Y','now')-strftime('%Y',date)) year) end as next_bd FROM Birthday order by next_bd;")
    fun getOrderedBirthdays(): List<Birthday>
}