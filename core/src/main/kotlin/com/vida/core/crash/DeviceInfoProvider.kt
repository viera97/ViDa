package com.vida.core.crash

import android.content.pm.PackageManager
import android.os.Build

/**
 * Provides device and app version information for crash reports.
 *
 * All values are available synchronously — safe to call from the crash
 * handler thread.
 */
interface DeviceInfoProvider {
    val appVersionName: String
    val appVersionCode: Long
    val deviceModel: String
    val osVersion: String
}

/**
 * Android implementation backed by [PackageManager] and [Build].
 */
class AndroidDeviceInfoProvider(
    private val packageManager: PackageManager,
    private val packageName: String,
) : DeviceInfoProvider {

    override val appVersionName: String by lazy {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    override val appVersionCode: Long by lazy {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.longVersionCode
        } catch (_: Exception) {
            0L
        }
    }

    override val deviceModel: String get() = "${Build.MANUFACTURER} ${Build.MODEL}"

    override val osVersion: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
