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
 * Modal bottom sheet for selecting a currency from a list of codes.
 *
 * Displays each currency code as a chip in an adaptive grid. The currently
 * selected currency is highlighted with primary color. Tapping a chip
 * selects it and invokes [onCurrencySelected].
 *
 * @param availableCurrencies List of currency codes to display.
 * @param selectedCurrencyCode The currently selected currency code, or null.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onCurrencySelected Callback when a currency is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    availableCurrencies: List<String>,
    selectedCurrencyCode: String?,
    onDismiss: () -> Unit,
    onCurrencySelected: (String) -> Unit,
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
                items = availableCurrencies.distinct(),
                key = { it },
            ) { code ->
                val selected = code == selectedCurrencyCode
                val shape = RoundedCornerShape(12.dp)
                Surface(
                    onClick = {
                        onCurrencySelected(code)
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
                            text = code,
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
