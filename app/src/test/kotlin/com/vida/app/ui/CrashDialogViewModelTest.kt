package com.vida.app.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import app.cash.turbine.test
import com.vida.core.crash.CrashReport
import com.vida.core.crash.CrashReportStore
import com.vida.core.crash.ReportType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CrashDialogViewModelTest {

    private lateinit var application: Application
    private lateinit var store: CrashReportStore
    private lateinit var packageManager: PackageManager

    private val fatalReport = CrashReport(
        type = ReportType.FATAL,
        stackTrace = "java.lang.RuntimeException: crash",
        appVersionName = "1.0",
        appVersionCode = 1L,
        deviceModel = "Test Device",
        osVersion = "Android 99",
        timestamp = 1000L,
        screenName = "home",
        tag = null,
    )

    private val errorReport = CrashReport(
        type = ReportType.ERROR,
        stackTrace = "java.lang.IllegalStateException: error",
        appVersionName = "1.0",
        appVersionCode = 1L,
        deviceModel = "Test Device",
        osVersion = "Android 99",
        timestamp = 1001L,
        screenName = "settings",
        tag = "FeatureX",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        packageManager = mockk(relaxed = true)
        application = mockk<Application>(relaxed = true) {
            every { packageManager } returns this@setup.packageManager
        }
        store = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init migrates pending crash`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(null)

        CrashDialogViewModel(application, store)

        coVerify { store.migratePendingCrash() }
    }

    @Test
    fun `init shows dialog when fatal report is pending`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(fatalReport)

        val vm = CrashDialogViewModel(application, store)

        vm.state.test {
            val state = awaitItem()
            assertEquals(true, state.showDialog)
            assertEquals(fatalReport, state.report)
            assertEquals("La app crasheó. ¿Quieres enviar un reporte?", state.dialogMessage)
            cancel()
        }
    }

    @Test
    fun `init shows dialog with error message when error report is pending`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(errorReport)

        val vm = CrashDialogViewModel(application, store)

        vm.state.test {
            val state = awaitItem()
            assertEquals(true, state.showDialog)
            assertEquals(errorReport, state.report)
            assertEquals("Ocurrió un error en la app. ¿Quieres enviar un reporte?", state.dialogMessage)
            cancel()
        }
    }

    @Test
    fun `init does not show dialog when no report is pending`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(null)

        val vm = CrashDialogViewModel(application, store)

        vm.state.test {
            val state = awaitItem()
            assertEquals(false, state.showDialog)
            cancel()
        }
    }

    @Test
    fun `send launches ACTION_SENDTO intent and clears report`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(fatalReport)
        every { packageManager.resolveActivity(any<Intent>(), any()) } returns mockk()

        val vm = CrashDialogViewModel(application, store)

        // Wait for init to process.
        vm.state.test {
            awaitItem() // initial state with dialog visible
        }

        vm.send()

        coVerify { store.clearReport() }

        val intentSlot = slot<Intent>()
        verify { application.startActivity(capture(intentSlot)) }
        val intent = intentSlot.captured
        assertEquals(Intent.ACTION_SENDTO, intent.action)
        assertNotNull(intent.data)
        val recipients = intent.getStringArrayExtra(Intent.EXTRA_EMAIL)
        assertEquals(1, recipients!!.size)
        assertEquals("d.viera1997@gmail.com", recipients[0])
    }

    @Test
    fun `send clears report even when no email app is available`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(fatalReport)
        every { packageManager.resolveActivity(any<Intent>(), any()) } returns null

        val vm = CrashDialogViewModel(application, store)

        vm.state.test {
            awaitItem() // initial state with dialog visible
        }

        vm.send()

        coVerify { store.clearReport() }
    }

    @Test
    fun `dismiss clears report without launching intent`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(fatalReport)

        val vm = CrashDialogViewModel(application, store)

        vm.state.test {
            awaitItem() // initial state with dialog visible
        }

        vm.dismiss()

        coVerify { store.clearReport() }
    }

    @Test
    fun `dialog state returns to default after dismiss`() = runTest {
        coEvery { store.migratePendingCrash() } returns null
        coEvery { store.pendingReport() } returns flowOf(fatalReport, null)

        val vm = CrashDialogViewModel(application, store)

        vm.state.test {
            // First item: dialog shown.
            val shown = awaitItem()
            assertEquals(true, shown.showDialog)

            vm.dismiss()

            // After dismiss + store emits null, dialog should hide.
            val hidden = awaitItem()
            assertEquals(false, hidden.showDialog)
            cancel()
        }
    }
}
