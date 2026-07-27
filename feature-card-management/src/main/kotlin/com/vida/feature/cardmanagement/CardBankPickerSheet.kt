package com.vida.feature.cardmanagement

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
 * Modal bottom sheet for selecting a bank from a list.
 *
 * Displays each bank name as a chip in an adaptive grid. The currently
 * selected bank is highlighted with [MaterialTheme.colorScheme.primary].
 *
 * @param availableBanks List of bank names to display.
 * @param selectedBank The currently selected bank name, or empty.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onBankSelected Callback when a bank is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardBankPickerSheet(
    availableBanks: List<String>,
    selectedBank: String,
    onDismiss: () -> Unit,
    onBankSelected: (String) -> Unit,
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
                items = availableBanks.distinct(),
                key = { it },
            ) { bank ->
                val selected = bank == selectedBank
                val shape = RoundedCornerShape(12.dp)
                Surface(
                    onClick = {
                        onBankSelected(bank)
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
                            text = bank,
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
