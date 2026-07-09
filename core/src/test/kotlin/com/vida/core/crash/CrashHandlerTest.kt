package com.vida.core.crash

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class CrashHandlerTest {

    private lateinit var context: Context
    private lateinit var filesDir: File
    private lateinit var deviceInfoProvider: DeviceInfoProvider
    private lateinit var handler: CrashHandler

    @Before
    fun setup() {
        filesDir = File(System.getProperty("java.io.tmpdir"), "crash_test_${System.nanoTime()}")
        filesDir.mkdirs()

        context = mockk<Context>(relaxed = true) {
            every { filesDir } returns this@setup.filesDir
        }

        deviceInfoProvider = mockk {
            every { appVersionName } returns "1.0"
            every { appVersionCode } returns 1L
            every { deviceModel } returns "Test Manufacturer TestModel"
            every { osVersion } returns "Android 99 (API 99)"
        }

        handler = CrashHandler(context, deviceInfoProvider)
    }

    @After
    fun tearDown() {
        filesDir.deleteRecursively()
        // Restore the default handler to avoid polluting other tests.
        Thread.setDefaultUncaughtExceptionHandler(null)
    }

    @Test
    fun `register sets handler as default uncaught exception handler`() {
        handler.register()
        assertEquals(handler, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun `uncaughtException writes crash file with valid JSON`() {
        CurrentScreenTracker.currentScreen = "home"
        handler.register()

        val throwable = RuntimeException("test crash")
        // Capture the previous handler call to avoid System.exit side effects.
        val previousSlot = slot<Thread.UncaughtExceptionHandler>()
        Thread.setDefaultUncaughtExceptionHandler(mockk(relaxed = true))
        handler.register()

        handler.uncaughtException(Thread.currentThread(), throwable)

        val crashFile = File(filesDir, "last_crash.json")
        assertTrue("Crash file should exist", crashFile.exists())

        val json = crashFile.readText()
        val report = CrashReport.fromJson(json)
        assertEquals(ReportType.FATAL, report!!.type)
        assertTrue(report.stackTrace.contains("test crash"))
        assertEquals("1.0", report.appVersionName)
        assertEquals(1L, report.appVersionCode)
        assertEquals("Test Manufacturer TestModel", report.deviceModel)
        assertEquals("Android 99 (API 99)", report.osVersion)
        assertEquals("home", report.screenName)
    }

    @Test
    fun `uncaughtException writes null screenName when no screen is set`() {
        CurrentScreenTracker.currentScreen = null
        handler.register()

        val throwable = RuntimeException("crash before screen")
        Thread.setDefaultUncaughtExceptionHandler(mockk(relaxed = true))
        handler.register()

        handler.uncaughtException(Thread.currentThread(), throwable)

        val crashFile = File(filesDir, "last_crash.json")
        val report = CrashReport.fromJson(crashFile.readText())
        assertEquals(null, report!!.screenName)
    }

    @Test
    fun `uncaughtException delegates to previous handler after saving`() {
        val previousHandler = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        handler.register()

        val thread = Thread.currentThread()
        val throwable = RuntimeException("test")
        handler.uncaughtException(thread, throwable)

        verify { previousHandler.uncaughtException(thread, throwable) }
    }

    @Test
    fun `uncaughtException still delegates when save fails`() {
        // Set up context that throws on filesDir access.
        val brokenContext = mockk<Context>(relaxed = true) {
            every { filesDir } throws RuntimeException("Storage unavailable")
        }
        val brokenHandler = CrashHandler(brokenContext, deviceInfoProvider)

        val previousHandler = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        brokenHandler.register()

        val thread = Thread.currentThread()
        val throwable = RuntimeException("test")
        brokenHandler.uncaughtException(thread, throwable)

        // Even though save failed, the previous handler must still be called.
        verify { previousHandler.uncaughtException(thread, throwable) }
    }

    @Test
    fun `uncaughtException does not crash when no previous handler`() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        handler.register()

        // Should not throw even though previous handler is null.
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test"))
    }
}
