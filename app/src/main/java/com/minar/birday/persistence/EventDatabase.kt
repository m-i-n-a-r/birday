package com.minar.birday.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minar.birday.model.Event


@Database(entities = [Event::class], version = 11, exportSchema = false)
@TypeConverters(LocalDateTypeConverter::class)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        // Migration strategy to add two columns from version 9 to 10
        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE Event ADD COLUMN notes TEXT DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE Event ADD COLUMN image BLOB"
                )
            }
        }
        // Migration strategy to uppercase the type from version 10 to 11
        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE Event SET type = UPPER(type)"
                )
            }
        }
        fun getBirdayDatabase(context: Context): EventDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "BirdayDB"
                )
                    .addMigrations(MIGRATION_9_10)
                    .addMigrations(MIGRATION_10_11)
                    .build()
                INSTANCE = instance
                return instance
            }
        }

        fun destroyInstance() {
            if (INSTANCE?.isOpen == true) {
                INSTANCE?.close()
            }
            INSTANCE = null
        }
    }
}