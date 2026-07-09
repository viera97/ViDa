package com.vida.feature.home.update

/**
 * Single release asset exposed by the GitHub releases API.
 *
 * @property name Asset file name (e.g. `app-release.apk`).
 * @property sizeBytes Asset size in bytes as reported by GitHub.
 * @property downloadUrl Direct download URL (`browser_download_url`).
 */
data class ReleaseAsset(
    val name: String,
    val sizeBytes: Long,
    val downloadUrl: String,
)

/**
 * Outcome of a `check` against the GitHub releases endpoint.
 *
 * Sealed so the caller must handle both branches. The `htmlUrl` is included
 * for the up-to-date case so a future "see release notes" affordance can be
 * added without changing the model.
 */
sealed interface UpdateCheckResult {
    /** The installed version is >= the latest published version. */
    data class UpToDate(val currentVersion: String) : UpdateCheckResult

    /**
     * A newer version is available.
     *
     * @property version The latest release tag, with the leading `v` stripped.
     * @property asset The downloadable APK asset. `null` when the release was
     *   published without an `app-release.apk` asset (the UI shows a release-
     *   notes fallback).
     * @property htmlUrl Browser URL for the release page on GitHub.
     */
    data class Available(
        val version: String,
        val asset: ReleaseAsset?,
        val htmlUrl: String,
    ) : UpdateCheckResult
}

/**
 * Snapshot of an in-flight download. Reported to the UI for progress rendering.
 *
 * @property bytesDownloaded Bytes written to disk so far.
 * @property totalBytes Total bytes expected, as reported by the `Content-Length`
 *   response header. May be `0L` if the server did not send it.
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
) {
    /** Progress in the range `[0f, 1f]`. Returns `0f` when total is unknown. */
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f
        else (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
}
