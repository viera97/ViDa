package com.vida.feature.categorymanagement

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vida.core.icon.CATEGORY_ICONS
import com.vida.core.icon.CategoryIcon

/**
 * Alert dialog that displays a grid of available [CategoryIcon]s for selection.
 *
 * The user taps an icon to select it (highlighted with a primary border) and
 * presses "Aceptar" to confirm. The currently saved icon is pre-selected
 * via [selectedName].
 *
 * @param selectedName The name of the currently selected icon, or null.
 * @param onDismiss Invoked when the dialog is dismissed without saving.
 * @param onIconSelected Invoked with the [CategoryIcon] the user chose.
 */
@Composable
fun IconPickerDialog(
    selectedName: String?,
    onDismiss: () -> Unit,
    onIconSelected: (CategoryIcon) -> Unit,
) {
    var currentSelection by remember { mutableStateOf(selectedName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir icono") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = CATEGORY_ICONS,
                    key = { it.name },
                ) { categoryIcon ->
                    val isSelected = categoryIcon.name == currentSelection
                    val shape = RoundedCornerShape(12.dp)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = shape,
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { currentSelection = categoryIcon.name },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIcon.icon,
                            contentDescription = categoryIcon.name,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.then(
                                if (isSelected) Modifier else Modifier,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = CATEGORY_ICONS.firstOrNull { it.name == currentSelection }
                    if (selected != null) {
                        onIconSelected(selected)
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
