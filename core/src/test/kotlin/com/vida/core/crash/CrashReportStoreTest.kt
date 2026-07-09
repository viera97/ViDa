package com.vida.core.crash

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

class CrashReportStoreTest {

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: CrashReportStore
    private lateinit var filesDir: File

    private val sampleReport = CrashReport(
        type = ReportType.FATAL,
        stackTrace = "java.lang.RuntimeException: test",
        appVersionName = "1.0",
        appVersionCode = 1L,
        deviceModel = "Test Device",
        osVersion = "Android 99",
        timestamp = 1000L,
        screenName = "test_screen",
        tag = null,
    )

    @Before
    fun setup() {
        filesDir = File(System.getProperty("java.io.tmpdir"), "crash_test_${System.nanoTime()}")
        filesDir.mkdirs()

        context = mockk<Context>(relaxed = true) {
            every { filesDir } returns this@setup.filesDir
        }

        dataStore = InMemoryDataStore()
        store = CrashReportStore(context, dataStore)
    }

    @After
    fun tearDown() {
        filesDir.deleteRecursively()
    }

    @Test
    fun `pendingReport emits null when no report saved`() = runTest {
        store.pendingReport().test {
            assertEquals(null, awaitItem())
            cancel()
        }
    }

    @Test
    fun `saveReport then pendingReport emits the report`() = runTest {
        store.saveReport(sampleReport)

        store.pendingReport().test {
            val report = awaitItem()
            assertEquals(sampleReport, report)
            cancel()
        }
    }

    @Test
    fun `saveReport overwrites previous report`() = runTest {
        val first = sampleReport.copy(timestamp = 100L)
        val second = sampleReport.copy(timestamp = 200L)

        store.saveReport(first)
        store.saveReport(second)

        store.pendingReport().test {
            val report = awaitItem()
            assertEquals(200L, report!!.timestamp)
            cancel()
        }
    }

    @Test
    fun `clearReport removes pending report`() = runTest {
        store.saveReport(sampleReport)
        store.clearReport()

        store.pendingReport().test {
            assertEquals(null, awaitItem())
            cancel()
        }
    }

    @Test
    fun `migratePendingCrash returns report from file and deletes file`() = runTest {
        val crashFile = File(filesDir, "last_crash.json")
        crashFile.writeText(sampleReport.toJson())

        val migrated = store.migratePendingCrash()

        assertEquals(sampleReport, migrated)
        // File should be deleted after migration.
        assertEquals(false, crashFile.exists())
    }

    @Test
    fun `migratePendingCrash also saves to DataStore`() = runTest {
        val crashFile = File(filesDir, "last_crash.json")
        crashFile.writeText(sampleReport.toJson())

        store.migratePendingCrash()

        val stored = store.pendingReport().first()
        assertEquals(sampleReport, stored)
    }

    @Test
    fun `migratePendingCrash returns null when no file exists`() = runTest {
        val result = store.migratePendingCrash()
        assertNull(result)
    }

    @Test
    fun `migratePendingCrash returns null and deletes file when JSON is invalid`() = runTest {
        val crashFile = File(filesDir, "last_crash.json")
        crashFile.writeText("not valid json{")

        val result = store.migratePendingCrash()

        assertNull(result)
        assertEquals(false, crashFile.exists())
    }
}

/**
 * Simple in-memory replacement for Android's DataStore, backed by a MutableMap.
 * Only implements the subset of DataStore needed for CrashReportStore tests.
 */
private class InMemoryDataStore : DataStore<Preferences> {
    private val delegate = androidx.datastore.preferences.core.mutablePreferencesOf()

    override val data = kotlinx.coroutines.flow.flow {
        emit(delegate)
    }

    override suspend fun <T> edit(transform: suspend (Preferences.MutablePreferences) -> T): T {
        val mutable = delegate.toMutablePreferences()
        val result = transform(mutable)
        delegate.clear()
        delegate.putAll(mutable)
        return result
    }
}
