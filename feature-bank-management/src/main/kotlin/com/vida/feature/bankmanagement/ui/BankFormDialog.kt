package com.vida.feature.bankmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Full-screen dialog for creating or editing a bank.
 *
 * Shows an [OutlinedTextField] for the name (pre-filled when editing)
 * and a 3×4 grid of selectable color circles (no icon picker).
 * The save button is disabled when the name is blank or a save operation
 * is in progress.
 *
 * @param initialName Pre-populated name (empty for new banks).
 * @param initialColor ARGB int for the pre-selected color dot.
 * @param isEdit `true` to show "Editar banco" title, `false` for "Nuevo banco".
 * @param isSaving When `true` the save button and inputs are disabled.
 * @param onDismiss Invoked on cancel or tap-outside.
 * @param onSave Invoked with `(name, color)` when the user taps "Guardar".
 */
@Composable
fun BankFormDialog(
    initialName: String = "",
    initialColor: Int = PRESET_COLORS[0],
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Editar banco" else "Nuevo banco")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3 rows × 4 columns of preset colors
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (col in 0..3) {
                            val color = PRESET_COLORS[row * 4 + col]
                            val isSelected = color == selectedColor

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable(enabled = !isSaving) {
                                        selectedColor = color
                                    },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, selectedColor) },
                enabled = isValid && !isSaving,
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

// ── Companion: preset color palette ──────────────────────────────────────────

/**
 * Twelve preset ARGB colors available in the color grid.
 *
 * Colors reference: [Material Design 2014 palette](https://m3.material.io/styles/color/the-color-system/key-colors-tones).
 */
val PRESET_COLORS: List<Int> = listOf(
    0xFFF44336.toInt(), // Red
    0xFFFF9800.toInt(), // Orange
    0xFFFFC107.toInt(), // Amber
    0xFF4CAF50.toInt(), // Green
    0xFF009688.toInt(), // Teal
    0xFF00BCD4.toInt(), // Cyan
    0xFF2196F3.toInt(), // Blue
    0xFF3F51B5.toInt(), // Indigo
    0xFF9C27B0.toInt(), // Purple
    0xFFE91E63.toInt(), // Pink
    0xFF795548.toInt(), // Brown
    0xFF9E9E9E.toInt(), // Grey
)
