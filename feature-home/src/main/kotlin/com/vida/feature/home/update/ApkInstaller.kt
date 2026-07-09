package com.vida.feature.home.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens the system APK installer for a previously downloaded file.
 *
 * Keeps Android-specific side effects (Intents, FileProvider URIs) out of
 * [UpdateManager], which stays a pure I/O class and is easier to unit test.
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Fires the `ACTION_VIEW` intent that hands the downloaded APK to the
     * system installer. Throws [IllegalStateException] if no installer
     * activity is registered on the device.
     */
    fun install(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.vida.app.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            throw IllegalStateException("No hay instalador disponible en el dispositivo", e)
        }
    }
}
