package com.vida.core.crash

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultCrashReporterTest {

    private lateinit var store: CrashReportStore
    private lateinit var deviceInfoProvider: DeviceInfoProvider
    private lateinit var reporter: DefaultCrashReporter

    @Before
    fun setup() {
        store = mockk(relaxed = true)
        deviceInfoProvider = mockk {
            every { appVersionName } returns "1.0"
            every { appVersionCode } returns 1L
            every { deviceModel } returns "Test Manufacturer TestModel"
            every { osVersion } returns "Android 99 (API 99)"
        }
        reporter = DefaultCrashReporter(store, deviceInfoProvider)
    }

    @Test
    fun `reportError saves crash report with ERROR type`() = runTest {
        val throwable = IllegalStateException("Something went wrong")

        reporter.reportError("FeatureX", throwable, "settings")

        coVerify {
            store.saveReport(match { report ->
                report.type == ReportType.ERROR &&
                    report.tag == "FeatureX" &&
                    report.screenName == "settings" &&
                    report.stackTrace.contains("Something went wrong") &&
                    report.appVersionName == "1.0" &&
                    report.appVersionCode == 1L &&
                    report.deviceModel == "Test Manufacturer TestModel" &&
                    report.osVersion == "Android 99 (API 99)"
            })
        }
    }

    @Test
    fun `reportError saves report with null screenName when not provided`() = runTest {
        val throwable = RuntimeException("error")

        reporter.reportError("FeatureY", throwable)

        coVerify {
            store.saveReport(match { report ->
                report.type == ReportType.ERROR &&
                    report.tag == "FeatureY" &&
                    report.screenName == null
            })
        }
    }
}
