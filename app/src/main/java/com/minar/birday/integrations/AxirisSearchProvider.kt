package com.minar.birday.integrations

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.preference.PreferenceManager
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.persistence.EventDatabase
import com.minar.birday.utilities.formatName
import com.minar.birday.utilities.getReducedDate
import com.minar.birday.utilities.getRemainingDays
import com.minar.birday.utilities.getStringForTypeCodename
import java.text.Normalizer

/**
 * Birday's events in the Axiris launcher search.
 *
 * Implements the Axiris integration contract (github.com/m-i-n-a-r/yawncher, docs/INTEGRATIONS.md):
 * one read-only query, content://<authority>/search?q=...&limit=..., answered only when the caller
 * is Axiris. Each row is an event: the name, its type and next date, how long until it, its photo,
 * and an intent that opens the event's details in Birday.
 */
class AxirisSearchProvider : ContentProvider() {

    override fun onCreate() = true

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        if (callingPackage !in AXIRIS) return null
        if (uri.lastPathSegment != "search") return null
        val context = context ?: return null
        val query = fold(uri.getQueryParameter("q").orEmpty().trim())
        if (query.isEmpty()) return null
        val limit = uri.getQueryParameter("limit")?.toIntOrNull()?.coerceIn(1, 20) ?: 5

        val surnameFirst = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("surname_first", false)
        val cursor = MatrixCursor(COLUMNS)
        EventDatabase.getBirdayDatabase(context).eventDao().getOrderedEventsStatic()
            .asSequence()
            .filter { fold("${it.name} ${it.surname.orEmpty()}").contains(query) }
            .take(limit)
            .forEach { event ->
                val next = event.nextDate ?: return@forEach
                // Birday's own words for it, already translated. The system's relative time turns
                // into a plain date past a week, which is exactly when a countdown matters less
                val until = when (val days = getRemainingDays(next)) {
                    0 -> context.getString(R.string.today)
                    1 -> context.getString(R.string.tomorrow)
                    else -> context.resources.getQuantityString(R.plurals.days_left, days, days)
                }
                cursor.addRow(
                    arrayOf(
                        event.id.toString(),
                        formatName(event, surnameFirst),
                        getStringForTypeCodename(context, event.type ?: "BIRTHDAY") + " · " + getReducedDate(next),
                        until,
                        event.image,
                        "round",
                        Intent(context, MainActivity::class.java)
                            .putExtra(MainActivity.EXTRA_EVENT_ID, event.id)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .toUri(Intent.URI_INTENT_SCHEME)
                    )
                )
            }
        return cursor
    }

    /** Lower case and without accents, so "jose" finds "José". */
    private fun fold(text: String) =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(MARKS, "").lowercase()

    // Read only: Axiris never writes
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    private companion object {
        val AXIRIS = setOf("com.minar.axiris", "com.minar.axiris.dev")
        val COLUMNS = arrayOf("id", "title", "subtitle", "extra", "image", "image_shape", "intent")
        val MARKS = Regex("\\p{Mn}+")
    }
}
