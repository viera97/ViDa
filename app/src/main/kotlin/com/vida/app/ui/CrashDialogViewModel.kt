package com.vida.app.ui

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.core.crash.CrashReport
import com.vida.core.crash.CrashReportStore
import com.vida.core.crash.ReportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the crash/error dialog.
 */
data class CrashDialogState(
    val showDialog: Boolean = false,
    val report: CrashReport? = null,
    val dialogMessage: String = "",
)

/**
 * ViewModel that manages the crash/error dialog lifecycle.
 *
 * On init:
 * 1. Calls [CrashReportStore.migratePendingCrash] to move any file-based
 *    fatal crash into DataStore.
 * 2. Observes [CrashReportStore.pendingReport] to show the dialog when a
 *    report is present.
 *
 * User actions:
 * - [send]: builds an [Intent.ACTION_SENDTO] email with report body, clears
 *   the report, dismisses the dialog.
 * - [dismiss]: clears the report and dismisses the dialog.
 *
 * The report is cleared in ALL cases (send or dismiss) so the dialog never
 * appears twice for the same report.
 */
@HiltViewModel
class CrashDialogViewModel @Inject constructor(
    private val application: Application,
    private val store: CrashReportStore,
) : ViewModel() {

    private val _state = MutableStateFlow(CrashDialogState())
    val state: StateFlow<CrashDialogState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Migrate any fatal crash file into DataStore on first launch.
            store.migratePendingCrash()

            // Observe pending reports and show dialog when present.
            store.pendingReport().collect { report ->
                if (report != null) {
                    val message = dialogMessageFor(report)
                    _state.value = CrashDialogState(
                        showDialog = true,
                        report = report,
                        dialogMessage = message,
                    )
                }
            }
        }
    }

    /**
     * Builds and launches an [Intent.ACTION_SENDTO] email with the report
     * body, clears the report from DataStore, then dismisses the dialog.
     *
     * If no email app is available, shows a Toast instead.
     */
    fun send() {
        val report = _state.value.report ?: return

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(RECIPIENT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, emailSubjectFor(report))
            putExtra(Intent.EXTRA_TEXT, emailBodyFor(report))
        }

        if (intent.resolveActivity(application.packageManager) != null) {
            application.startActivity(intent)
        } else {
            Toast.makeText(
                application,
                "Report saved. Contact support.",
                Toast.LENGTH_LONG,
            ).show()
        }

        clearAndDismiss()
    }

    /**
     * Clears the report from DataStore and dismisses the dialog without
     * sending anything.
     */
    fun dismiss() {
        clearAndDismiss()
    }

    private fun clearAndDismiss() {
        viewModelScope.launch {
            store.clearReport()
            _state.value = CrashDialogState()
        }
    }

    private fun dialogMessageFor(report: CrashReport): String = when (report.type) {
        ReportType.FATAL -> "La app crasheó. ¿Quieres enviar un reporte?"
        ReportType.ERROR -> "Ocurrió un error en la app. ¿Quieres enviar un reporte?"
    }

    private fun emailSubjectFor(report: CrashReport): String = when (report.type) {
        ReportType.FATAL -> "[ViDa] Reporte de Crash"
        ReportType.ERROR -> "[ViDa] Reporte de Error"
    }

    private fun emailBodyFor(report: CrashReport): String = buildString {
        appendLine("Tipo: ${report.type.name}")
        report.tag?.let { appendLine("Tag: $it") }
        appendLine()
        appendLine("--- Contexto ---")
        appendLine("Versión: ${report.appVersionName} (${report.appVersionCode})")
        appendLine("Dispositivo: ${report.deviceModel}")
        appendLine("OS: ${report.osVersion}")
        appendLine("Timestamp: ${report.timestamp}")
        report.screenName?.let { appendLine("Pantalla: $it") }
        appendLine()
        appendLine("--- Stack Trace ---")
        appendLine(report.stackTrace)
    }

    private companion object {
        const val RECIPIENT_EMAIL = "d.viera1997@gmail.com"
    }
}
