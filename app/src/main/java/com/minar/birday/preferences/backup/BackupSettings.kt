package com.minar.birday.preferences.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File


// The settings travel in the Birday backup, in a table of their own inside the same database
// file. Room ignores the tables it doesn't know, so older versions import such a backup exactly as
// before, and a backup made before this table simply brings no settings back

private const val SETTINGS_TABLE = "BackupSettings"

// Only what the user chose. The inner state of the app, the counters and anything tied to this
// device, as the folder of the automatic export, stay out
private val BACKED_UP_KEYS = setOf(
    "accent_color", "additional_only_favorites", "advanced_overview", "alternative_calendar",
    "amoled_dark", "angry_bird", "auto_import", "contacts_full_name", "days_milestones",
    "delete_search", "disable_astrology", "edge_blur", "grouped_notifications", "hide_images",
    "hide_scroll", "leap_year_feb28", "loop_avd", "multi_additional_notification",
    "notification_hour", "notification_minute", "notification_only_favorites",
    "order_alphabetically", "overview_scale", "replace_on_conflict", "shimmer", "surname_first",
    "theme_color", "unbirthdays", "user_birthday", "user_name", "vibration",
)
private val BACKED_UP_PREFIXES = listOf("widget_compact_", "widget_minimal_")

// A string set in a single value, split by a character no setting contains
private const val SET_SEPARATOR = "\u001F"

private fun isBackedUp(key: String) =
    key in BACKED_UP_KEYS || BACKED_UP_PREFIXES.any { key.startsWith(it) }

// Called right before the database is copied, on the database the copy is made from
fun writeBackupSettings(context: Context, database: SupportSQLiteDatabase) {
    database.execSQL(
        "CREATE TABLE IF NOT EXISTS $SETTINGS_TABLE " +
            "(`key` TEXT NOT NULL PRIMARY KEY, `type` TEXT NOT NULL, `value` TEXT NOT NULL)"
    )
    database.execSQL("DELETE FROM $SETTINGS_TABLE")
    val settings = PreferenceManager.getDefaultSharedPreferences(context).all
    for ((key, value) in settings) {
        if (!isBackedUp(key) || value == null) continue
        val (type, text) = when (value) {
            is Boolean -> "boolean" to value.toString()
            is Int -> "int" to value.toString()
            is Long -> "long" to value.toString()
            is Float -> "float" to value.toString()
            is String -> "string" to value
            is Set<*> -> "set" to value.joinToString(SET_SEPARATOR)
            else -> continue
        }
        database.execSQL(
            "INSERT OR REPLACE INTO $SETTINGS_TABLE (`key`, `type`, `value`) VALUES (?, ?, ?)",
            arrayOf(key, type, text)
        )
    }
}

// Called right after a backup is copied in place of the database. Nothing here may fail the
// import: the events are what matters, the settings come along if they can
fun restoreBackupSettings(context: Context, databaseFile: File) {
    runCatching {
        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val hasTable = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                arrayOf(SETTINGS_TABLE)
            ).use { it.moveToFirst() }
            if (!hasTable) return
            db.rawQuery("SELECT `key`, `type`, `value` FROM $SETTINGS_TABLE", null).use { cursor ->
                PreferenceManager.getDefaultSharedPreferences(context).edit {
                    while (cursor.moveToNext()) {
                        val key = cursor.getString(0)
                        val value = cursor.getString(2)
                        if (!isBackedUp(key)) continue
                        runCatching {
                            when (cursor.getString(1)) {
                                "boolean" -> putBoolean(key, value.toBoolean())
                                "int" -> putInt(key, value.toInt())
                                "long" -> putLong(key, value.toLong())
                                "float" -> putFloat(key, value.toFloat())
                                "string" -> putString(key, value)
                                "set" -> putStringSet(
                                    key, value.split(SET_SEPARATOR).filter { it.isNotEmpty() }.toSet()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
