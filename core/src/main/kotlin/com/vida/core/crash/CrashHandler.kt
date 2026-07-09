package com.vida.core.crash

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Thread.UncaughtExceptionHandler] that captures fatal unhandled exceptions,
 * writes a [CrashReport] to a JSON file synchronously, and then delegates to
 * the previous default handler.
 *
 * The JSON file is written to [Context.filesDir] so it survives the process
 * death. On the next app launch [CrashReportStore.migratePendingCrash] reads
 * it and moves the content into DataStore.
 *
 * The handler guards its own save logic with an inner try-catch to prevent
 * crash loops: if the save itself fails, the exception is logged to logcat
 * and the previous handler still runs.
 */
@Singleton
class CrashHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceInfoProvider: DeviceInfoProvider,
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_FILE_NAME = "last_crash.json"
    }

    private val previousHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    /**
     * Returns this instance so that [Thread.setDefaultUncaughtExceptionHandler]
     * can register it cleanly from [Application.onCreate].
     */
    fun register(): CrashHandler {
        Thread.setDefaultUncaughtExceptionHandler(this)
        return this
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = CrashReport(
                type = ReportType.FATAL,
                stackTrace = throwable.stackTraceToString(),
                appVersionName = deviceInfoProvider.appVersionName,
                appVersionCode = deviceInfoProvider.appVersionCode,
                deviceModel = deviceInfoProvider.deviceModel,
                osVersion = deviceInfoProvider.osVersion,
                timestamp = System.currentTimeMillis(),
                screenName = CurrentScreenTracker.currentScreen,
                tag = null,
            )
            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(report.toJson())
            Log.i(TAG, "Crash report saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report", e)
        }

        // Always delegate to the previous handler so the process terminates
        // and the system displays the standard "App has stopped" dialog.
        previousHandler?.uncaughtException(thread, throwable)
    }
}
