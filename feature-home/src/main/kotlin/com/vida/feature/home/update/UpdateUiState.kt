package com.vida.feature.home.update

import java.io.File

/**
 * State machine for the in-app update flow driven from the Home TopAppBar.
 *
 * Transitions are:
 * ```
 *   Idle → Checking
 *   Checking → UpToDate → Idle          (snackbar then auto-dismiss)
 *   Checking → UpdateAvailable          (alert dialog)
 *   UpdateAvailable → Downloading       (alert dialog with progress bar)
 *   UpdateAvailable → Idle              (user pressed Descartar)
 *   Downloading → ReadyToInstall        (alert dialog with Instalar button)
 *   Downloading → Error                 (snackbar, then auto-dismiss to Idle)
 *   Checking → Error                    (snackbar, then auto-dismiss to Idle)
 *   ReadyToInstall → Idle               (after Instalar is pressed)
 *   ReadyToInstall → Error              (if ApkInstaller throws)
 * ```
 */
sealed interface UpdateUiState {
    /** Initial state. No dialog, no spinner. */
    data object Idle : UpdateUiState

    /** Network call in flight. The trigger button should be disabled. */
    data object Checking : UpdateUiState

    /**
     * Server confirmed the installed version is the latest. The UI should
     * surface a snackbar and then call `dismissUpdateDialog` to revert to
     * [Idle].
     */
    data class UpToDate(val currentVersion: String) : UpdateUiState

    /**
     * A newer release is available. Rendered as a confirmation dialog with
     * Descartar / Descargar actions.
     */
    data class UpdateAvailable(
        val version: String,
        val sizeBytes: Long,
    ) : UpdateUiState

    /**
     * APK is being downloaded. Rendered as a dialog with a determinate
     * progress bar and a disabled `Instalar` button.
     */
    data class Downloading(val progress: Float) : UpdateUiState

    /**
     * APK is fully downloaded and ready to hand off to the system installer.
     */
    data class ReadyToInstall(val file: File) : UpdateUiState

    /**
     * Something went wrong. Rendered as a snackbar; UI reverts to [Idle]
     * after the snackbar is dismissed.
     */
    data class Error(val message: String) : UpdateUiState
}
