package com.vida.feature.home.update

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Looks up the currently installed app's `versionName`.
 *
 * Kept separate from [UpdateManager] so the manager itself does not depend on
 * Android `Context` — easier to unit test and to swap in a fake for tests.
 */
@Singleton
class VersionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * The installed app's `versionName`, or `"0.0.0"` if it cannot be resolved
     * (e.g. the package metadata is missing in a stripped build).
     */
    fun currentVersionName(): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: "0.0.0"
    } catch (_: PackageManager.NameNotFoundException) {
        "0.0.0"
    }
}

/**
 * Network + filesystem layer of the in-app updater.
 *
 * The manager is pure I/O — it does NOT depend on `Context` for the network and
 * file operations. The current version is read through [VersionProvider] and
 * the downloaded APK is written into the caller-supplied destination (the
 * caller — typically [HomeViewModel] — picks `cacheDir/updates/app-release.apk`).
 *
 * Single endpoint, single asset, no streaming JSON parser needed: the response
 * is parsed with [org.json.JSONObject].
 */
@Singleton
class UpdateManager @Inject constructor(
    private val client: OkHttpClient,
    private val versionProvider: VersionProvider,
) {

    /**
     * Hits the GitHub `releases/latest` endpoint and returns an
     * [UpdateCheckResult]. The comparison is semver-ish (`major.minor.patch`)
     * with a leading `v` stripped from the tag.
     */
    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub responded with HTTP ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Empty GitHub response body")

            val current = versionProvider.currentVersionName()
            parseRelease(body, current)
        }
    }

    /**
     * Downloads [asset] into [destFile] in chunks, invoking [onProgress] for
     * every chunk written. Returns [destFile] on success.
     *
     * The progress callback is invoked on the IO dispatcher; callers should
     * `withContext(Main)` inside it if they update Compose state.
     */
    suspend fun download(
        asset: ReleaseAsset,
        destFile: File,
        onProgress: suspend (DownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(asset.downloadUrl)
            .header("Accept", "application/octet-stream")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed with HTTP ${response.code}")
            }
            val body = response.body
                ?: throw IOException("Empty download body")

            destFile.parentFile?.mkdirs()
            val total = if (body.contentLength() > 0) body.contentLength() else asset.sizeBytes

            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var written = 0L
                    var lastEmit = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        // Emit at most ~30 times per second to keep the UI smooth
                        // without flooding the dispatcher with state updates.
                        if (written - lastEmit >= PROGRESS_CHUNK_BYTES) {
                            onProgress(DownloadProgress(written, total))
                            lastEmit = written
                        }
                    }

                    // Final progress tick so the bar reaches 1.0 even when the
                    // server sent no Content-Length and the loop never
                    // triggered an emit.
                    onProgress(DownloadProgress(written, total))
                }
            }
            destFile
        }
    }

    private fun parseRelease(body: String, currentVersion: String): UpdateCheckResult {
        return try {
            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val remote = stripLeadingV(tag)
            val htmlUrl = json.optString("html_url", "")

            val asset = findApkAsset(json)

            if (remote.isBlank() || isRemoteAtMost(remote, currentVersion)) {
                UpdateCheckResult.UpToDate(currentVersion)
            } else {
                UpdateCheckResult.Available(version = remote, asset = asset, htmlUrl = htmlUrl)
            }
        } catch (e: JSONException) {
            throw IOException("No se pudo interpretar la respuesta de GitHub", e)
        }
    }

    private fun findApkAsset(json: JSONObject): ReleaseAsset? {
        val assets = json.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val item = assets.optJSONObject(i) ?: continue
            val name = item.optString("name", "")
            if (name == APK_ASSET_NAME) {
                return ReleaseAsset(
                    name = name,
                    sizeBytes = item.optLong("size", 0L),
                    downloadUrl = item.optString("browser_download_url", ""),
                )
            }
        }
        return null
    }

    private fun stripLeadingV(tag: String): String =
        if (tag.startsWith("v", ignoreCase = true)) tag.substring(1) else tag

    /**
     * Returns true when [remote] is less than or equal to [current]. Missing
     * components default to `0` (e.g. `0.1` == `0.1.0`).
     */
    private fun isRemoteAtMost(remote: String, current: String): Boolean {
        val r = parseVersion(remote)
        val c = parseVersion(current)
        for (i in 0 until 3) {
            if (r[i] < c[i]) return true
            if (r[i] > c[i]) return false
        }
        return true
    }

    private fun parseVersion(version: String): IntArray {
        val parts = version.split('.', '-', '+')
        val out = IntArray(3)
        for (i in 0 until minOf(3, parts.size)) {
            out[i] = parts[i].toIntOrNull() ?: 0
        }
        return out
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/Viera97/ViDa/releases/latest"
        const val APK_ASSET_NAME = "app-release.apk"
        const val BUFFER_SIZE = 8 * 1024
        const val PROGRESS_CHUNK_BYTES = 64L * 1024
    }
}
