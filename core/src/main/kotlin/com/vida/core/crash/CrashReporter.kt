package com.vida.core.crash

/**
 * Injectable API for features to report non-fatal errors through the same
 * persistence and dialog pipeline used for fatal crashes.
 *
 * Call [reportError] from a coroutine-capable scope (e.g. ViewModel scope).
 * The report is persisted immediately to DataStore and will prompt the user
 * on next launch with the error variant dialog.
 */
interface CrashReporter {
    /**
     * Persists a non-fatal [throwable] with an identifying [tag] and optional
     * [screenName]. The report overwrites any previously stored report
     * (fatal or error) — only the latest report is kept.
     */
    suspend fun reportError(tag: String, throwable: Throwable, screenName: String? = null)
}

/**
 * Default implementation of [CrashReporter] that delegates to a
 * [CrashReportStore] for persistence.
 */
class DefaultCrashReporter(
    private val store: CrashReportStore,
    private val deviceInfoProvider: DeviceInfoProvider,
) : CrashReporter {

    override suspend fun reportError(tag: String, throwable: Throwable, screenName: String?) {
        val report = CrashReport(
            type = ReportType.ERROR,
            stackTrace = throwable.stackTraceToString(),
            appVersionName = deviceInfoProvider.appVersionName,
            appVersionCode = deviceInfoProvider.appVersionCode,
            deviceModel = deviceInfoProvider.deviceModel,
            osVersion = deviceInfoProvider.osVersion,
            timestamp = System.currentTimeMillis(),
            screenName = screenName,
            tag = tag,
        )
        store.saveReport(report)
    }
}
