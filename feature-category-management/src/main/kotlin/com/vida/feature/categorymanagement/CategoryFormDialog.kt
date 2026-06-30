package com.vida.feature.categorymanagement

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vida.core.icon.CategoryIcon
import com.vida.core.icon.iconNameToImageVector

/**
 * Full-screen dialog for creating or editing a category.
 *
 * Shows an [OutlinedTextField] for the name (pre-filled when editing),
 * a 3×4 grid of selectable color circles, and an icon picker section.
 * The save button is disabled when the name is blank or a save operation
 * is in progress.
 *
 * @param initialName Pre-populated name (empty for new categories).
 * @param initialColor ARGB int for the pre-selected color dot.
 * @param initialIcon Icon name string for the pre-selected icon (nullable).
 * @param isEdit `true` to show "Editar categoría" title, `false` for "Nueva categoría".
 * @param isSaving When `true` the save button and inputs are disabled.
 * @param onDismiss Invoked on cancel or tap-outside.
 * @param onSave Invoked with `(name, color, icon)` when the user taps "Guardar".
 */
@Composable
fun CategoryFormDialog(
    initialName: String = "",
    initialColor: Int = PRESET_COLORS[0],
    initialIcon: String? = null,
    isEdit: Boolean = false,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, color: Int, icon: String?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var showIconPicker by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Editar categoría" else "Nueva categoría")
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

                // ── Icon section ─────────────────────────────────────────
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Icono",
                    style = MaterialTheme.typography.labelLarge,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preview of selected icon
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selectedIcon != null) {
                            Icon(
                                imageVector = iconNameToImageVector(selectedIcon),
                                contentDescription = selectedIcon,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    TextButton(
                        onClick = { showIconPicker = true },
                        enabled = !isSaving,
                    ) {
                        Text("Elegir icono")
                    }

                    if (selectedIcon != null) {
                        TextButton(
                            onClick = { selectedIcon = null },
                            enabled = !isSaving,
                        ) {
                            Text("Quitar")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, selectedColor, selectedIcon) },
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

    // Icon picker dialog
    if (showIconPicker) {
        IconPickerDialog(
            selectedName = selectedIcon,
            onDismiss = { showIconPicker = false },
            onIconSelected = { categoryIcon: CategoryIcon ->
                selectedIcon = categoryIcon.name
                showIconPicker = false
            },
        )
    }
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
