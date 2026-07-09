package com.vida.core.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CrashReportTest {

    private val fullReport = CrashReport(
        type = ReportType.FATAL,
        stackTrace = "java.lang.NullPointerException\n\tat com.vida.app.ui.ViDaApp.onCreate(ViDaApp.kt:42)",
        appVersionName = "0.2.0",
        appVersionCode = 2L,
        deviceModel = "Google Pixel 8",
        osVersion = "Android 14 (API 34)",
        timestamp = 1700000000000L,
        screenName = "home",
        tag = null,
    )

    private val errorReport = CrashReport(
        type = ReportType.ERROR,
        stackTrace = "java.lang.IllegalStateException: Invalid state",
        appVersionName = "0.2.0",
        appVersionCode = 2L,
        deviceModel = "Google Pixel 8",
        osVersion = "Android 14 (API 34)",
        timestamp = 1700000000001L,
        screenName = "settings",
        tag = "FeatureX",
    )

    @Test
    fun `toJson and fromJson roundtrip preserves all fields for FATAL report`() {
        val json = fullReport.toJson()
        val parsed = CrashReport.fromJson(json)

        assertNotNull(parsed)
        assertEquals(fullReport, parsed)
    }

    @Test
    fun `toJson and fromJson roundtrip preserves all fields for ERROR report with tag`() {
        val json = errorReport.toJson()
        val parsed = CrashReport.fromJson(json)

        assertNotNull(parsed)
        assertEquals(errorReport, parsed)
    }

    @Test
    fun `fromJson returns null for blank input`() {
        assertNull(CrashReport.fromJson(""))
        assertNull(CrashReport.fromJson("   "))
    }

    @Test
    fun `fromJson returns null for malformed JSON`() {
        assertNull(CrashReport.fromJson("{invalid}"))
        assertNull(CrashReport.fromJson("not json at all"))
    }

    @Test
    fun `fromJson handles null screenName and tag`() {
        val report = fullReport.copy(screenName = null, tag = null)
        val json = report.toJson()
        val parsed = CrashReport.fromJson(json)

        assertNotNull(parsed)
        assertNull(parsed!!.screenName)
        assertNull(parsed.tag)
        assertEquals(report, parsed)
    }

    @Test
    fun `fromJson handles ERROR report with tag`() {
        val report = errorReport.copy(screenName = null)
        val json = report.toJson()
        val parsed = CrashReport.fromJson(json)

        assertNotNull(parsed)
        assertEquals(ReportType.ERROR, parsed!!.type)
        assertEquals("FeatureX", parsed.tag)
        assertNull(parsed.screenName)
    }
}
