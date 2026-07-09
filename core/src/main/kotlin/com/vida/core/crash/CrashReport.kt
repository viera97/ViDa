package com.vida.core.crash

import org.json.JSONObject

/**
 * Distinguishes fatal unhandled crashes from non-fatal feature-level errors.
 */
enum class ReportType {
    FATAL,
    ERROR,
}

/**
 * Immutable snapshot of a single crash or error report.
 *
 * Serialized with [org.json.JSONObject] (Android SDK built-in) to avoid
 * additional dependencies. See [toJson] and [fromJson].
 *
 * Privacy: contains technical data only — stack trace, version info, device
 * info, timestamp, and optional screen name. MUST NOT include account data,
 * balances, amounts, or screen content.
 *
 * @property type FATAL for unhandled crashes, ERROR for non-fatal feature errors.
 * @property stackTrace Full exception stack trace string.
 * @property appVersionName Human-readable version name (e.g. "1.2.3").
 * @property appVersionCode Numeric version code.
 * @property deviceModel Manufacturer and model (e.g. "Google Pixel 8").
 * @property osVersion Android version string (e.g. "Android 14 (API 34)").
 * @property timestamp UTC millis since epoch.
 * @property screenName Nullable — the route/screen active at crash time. Null if
 *   no screen was set (e.g. crash during app init before any navigation).
 * @property tag Non-null only for ERROR type. Identifies the feature or
 *   component that reported the error.
 */
data class CrashReport(
    val type: ReportType,
    val stackTrace: String,
    val appVersionName: String,
    val appVersionCode: Long,
    val deviceModel: String,
    val osVersion: String,
    val timestamp: Long,
    val screenName: String?,
    val tag: String?,
) {
    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_STACK_TRACE = "stackTrace"
        private const val KEY_APP_VERSION_NAME = "appVersionName"
        private const val KEY_APP_VERSION_CODE = "appVersionCode"
        private const val KEY_DEVICE_MODEL = "deviceModel"
        private const val KEY_OS_VERSION = "osVersion"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_SCREEN_NAME = "screenName"
        private const val KEY_TAG = "tag"

        /**
         * Deserializes a [CrashReport] from a [JSONObject] string.
         * Returns `null` when the string is blank or malformed.
         */
        fun fromJson(json: String): CrashReport? {
            if (json.isBlank()) return null
            return try {
                val obj = JSONObject(json)
                CrashReport(
                    type = ReportType.valueOf(obj.getString(KEY_TYPE)),
                    stackTrace = obj.getString(KEY_STACK_TRACE),
                    appVersionName = obj.getString(KEY_APP_VERSION_NAME),
                    appVersionCode = obj.getLong(KEY_APP_VERSION_CODE),
                    deviceModel = obj.getString(KEY_DEVICE_MODEL),
                    osVersion = obj.getString(KEY_OS_VERSION),
                    timestamp = obj.getLong(KEY_TIMESTAMP),
                    screenName = obj.optString(KEY_SCREEN_NAME, null)?.ifBlank { null },
                    tag = obj.optString(KEY_TAG, null)?.ifBlank { null },
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Serializes this report to a [JSONObject] string.
     */
    fun toJson(): String = JSONObject().apply {
        put(KEY_TYPE, type.name)
        put(KEY_STACK_TRACE, stackTrace)
        put(KEY_APP_VERSION_NAME, appVersionName)
        put(KEY_APP_VERSION_CODE, appVersionCode)
        put(KEY_DEVICE_MODEL, deviceModel)
        put(KEY_OS_VERSION, osVersion)
        put(KEY_TIMESTAMP, timestamp)
        put(KEY_SCREEN_NAME, screenName ?: JSONObject.NULL)
        put(KEY_TAG, tag ?: JSONObject.NULL)
    }.toString()
}
