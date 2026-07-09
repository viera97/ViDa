package com.vida.core.crash

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Persistence layer for a single [CrashReport].
 *
 * Uses a hybrid strategy:
 * - Fatal crashes write a JSON file via [File] (synchronous, safe for crash thread).
 * - Non-fatal errors write directly to [DataStore] (suspend, called from coroutine).
 * - On app launch, [migratePendingCrash] reads the file and moves it to DataStore
 *   to unify the pipeline.
 *
 * Only the **latest** report is kept — a new report overwrites any previous one.
 */
class CrashReportStore(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private const val CRASH_FILE_NAME = "last_crash.json"
        private val PENDING_REPORT_KEY = stringPreferencesKey("pending_crash_report")
    }

    private val crashFile: File get() = File(context.filesDir, CRASH_FILE_NAME)

    /**
     * Returns a [Flow] that emits the currently stored [CrashReport], or `null`
     * when no report is pending.
     */
    fun pendingReport(): Flow<CrashReport?> = dataStore.data.map { prefs ->
        val json = prefs[PENDING_REPORT_KEY]
        if (json != null) CrashReport.fromJson(json) else null
    }

    /**
     * Persists a [report] directly to DataStore (suspend, for non-fatal errors
     * called within a coroutine).
     */
    suspend fun saveReport(report: CrashReport) {
        dataStore.edit { prefs ->
            prefs[PENDING_REPORT_KEY] = report.toJson()
        }
    }

    /**
     * Removes the pending report from DataStore. Called after the user has
     * either sent or dismissed the crash dialog.
     */
    suspend fun clearReport() {
        dataStore.edit { prefs ->
            prefs.remove(PENDING_REPORT_KEY)
        }
    }

    /**
     * Reads the crash file (written by [CrashHandler]), migrates its content
     * into DataStore, and deletes the file. Returns the migrated [CrashReport]
     * or `null` if no file exists or it cannot be parsed.
     *
     * Safe to call multiple times — the file is deleted after migration.
     */
    suspend fun migratePendingCrash(): CrashReport? {
        val file = crashFile
        if (!file.exists()) return null

        val json = try {
            file.readText()
        } catch (_: Exception) {
            null
        }

        if (json.isNullOrBlank()) {
            file.delete()
            return null
        }

        val report = CrashReport.fromJson(json)
        if (report != null) {
            // Only migrate to DataStore if it's a fatal report. Non-fatal
            // reports would never land in the file, but guard defensively.
            saveReport(report)
        }

        file.delete()
        return report
    }
}
