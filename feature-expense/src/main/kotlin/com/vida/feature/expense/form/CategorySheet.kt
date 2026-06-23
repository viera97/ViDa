package com.vida.feature.expense.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Category

/**
 * Modal bottom sheet displaying a grid of category chips.
 *
 * Each category chip shows the category name on a background colored with
 * [Category.color]. The currently selected category is highlighted. Tapping
 * a chip selects it and invokes [onCategorySelected] with the category id.
 *
 * @param categories Available categories to display.
 * @param selectedId The currently selected category id, or null.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onCategorySelected Callback when a category chip is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheet(
    categories: List<Category>,
    selectedId: Long?,
    onDismiss: () -> Unit,
    onCategorySelected: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(
                count = categories.size,
                key = { categories[it].id },
            ) { index ->
                val category = categories[index]
                val isSelected = category.id == selectedId

                CategoryChip(
                    name = category.name,
                    color = Color(category.color),
                    selected = isSelected,
                    onClick = { onCategorySelected(category.id) },
                )
            }
        }
    }
}

/**
 * Individual category chip rendered in the [CategorySheet] grid.
 */
@Composable
private fun CategoryChip(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) color else color.copy(alpha = 0.15f),
        tonalElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier.padding(4.dp),
    ) {
        Box(
            modifier = Modifier.size(width = 120.dp, height = 56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
