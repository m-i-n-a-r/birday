package com.minar.birday.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Event::class], version = 8, exportSchema = false)
@TypeConverters(LocalDateTypeConverter::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        var INSTANCE: EventDatabase? = null

        fun getBirdayDataBase(context: Context): EventDatabase? {
            if (INSTANCE == null){
                synchronized(EventDatabase::class){
                    INSTANCE = Room.databaseBuilder(context.applicationContext, EventDatabase::class.java, "BirdayDB").build()
                }
            }
            return INSTANCE
        }

        fun destroyDataBase(){
            INSTANCE = null
        }
    }
}