package com.minar.birday.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Birthday::class], version = 3, exportSchema = false)
@TypeConverters(LocalDateTypeConverter::class)
abstract class BirdayDatabase : RoomDatabase() {
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        var INSTANCE: BirdayDatabase? = null

        fun getBirdayDataBase(context: Context): BirdayDatabase? {
            if (INSTANCE == null){
                synchronized(BirdayDatabase::class){
                    INSTANCE = Room.databaseBuilder(context.applicationContext, BirdayDatabase::class.java, "BirdayDB").build()
                }
            }
            return INSTANCE
        }

        fun destroyDataBase(){
            INSTANCE = null
        }
    }
}