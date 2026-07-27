package com.vida.feature.ratemanagement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Modal bottom sheet for selecting a provider from a list.
 *
 * Displays each provider as a chip in an adaptive grid. The currently
 * selected provider is highlighted with primary color. Tapping a chip
 * selects it and invokes [onProviderSelected].
 *
 * @param availableProviders List of provider names to display.
 * @param selectedProvider The currently selected provider, or null.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onProviderSelected Callback when a provider is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPickerSheet(
    availableProviders: List<String>,
    selectedProvider: String?,
    onDismiss: () -> Unit,
    onProviderSelected: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(
                items = availableProviders.distinct(),
                key = { it },
            ) { provider ->
                val selected = provider == selectedProvider
                val shape = RoundedCornerShape(12.dp)
                Surface(
                    onClick = {
                        onProviderSelected(provider)
                        onDismiss()
                    },
                    shape = shape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    tonalElevation = if (selected) 4.dp else 0.dp,
                    modifier = Modifier.padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier.size(width = 100.dp, height = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = provider,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}
