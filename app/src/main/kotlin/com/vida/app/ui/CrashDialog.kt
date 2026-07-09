package com.vida.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Crash/error dialog overlay.
 *
 * Renders an [AlertDialog] with the crash/error message when a pending report
 * exists. The dialog variant (fatal vs error) is determined by the report type.
 *
 * Place this composable at the root of the app (inside the theme wrapper) so it
 * overlays all screens.
 *
 * @param viewModel Injected via [hiltViewModel].
 */
@Composable
fun CrashDialog(
    viewModel: CrashDialogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.showDialog && state.report != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            title = { Text(text = "Reporte") },
            text = { Text(text = state.dialogMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.send() }) {
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismiss() }) {
                    Text("No enviar")
                }
            },
        )
    }
}
