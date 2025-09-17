package com.minar.birday.workers

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.minar.birday.persistence.ContactsRepository
import com.minar.birday.persistence.EventDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.content.Context
import androidx.core.content.edit

// Imports from contacts in background if the app is unused for more than a week
class ImportContactsWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val autoImport = prefs.getBoolean("auto_import", false)
        if (!autoImport) return Result.success()

        val lastLaunch = prefs.getLong("last_launch", 0L)
        val now = System.currentTimeMillis()
        val weekMillis = TimeUnit.DAYS.toMillis(7)

        // If the app has been launched in the last week, skip
        if (lastLaunch + weekMillis > now) {
            return Result.success()
        }

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        return try {
            val events = withContext(Dispatchers.IO) {
                ContactsRepository().getEventsFromContacts(applicationContext.contentResolver)
            }

            if (events.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    val db = EventDatabase.getBirdayDatabase(applicationContext)
                    val dao = db.eventDao()
                    val replaceOnConflict = prefs.getBoolean("replace_on_conflict", true)
                    if (replaceOnConflict) dao.insertAllEventReplace(events) else dao.insertAllEventIgnore(
                        events
                    )
                }
                prefs.edit { putLong("last_launch", now) }
            }

            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}
