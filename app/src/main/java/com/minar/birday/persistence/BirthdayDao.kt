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
}