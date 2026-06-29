package com.vida.feature.recurringexpensemanagement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vida.core.icon.iconNameToImageVector
import com.vida.domain.model.Category

/**
 * Modal bottom sheet displaying a grid of category chips.
 *
 * Each chip shows an icon (if available) and the category name on a background
 * tinted with [Category.color]. The currently selected category is highlighted
 * with full opacity + tonal elevation. Tapping a chip selects it and invokes
 * [onCategorySelected] with the category id.
 *
 * Mirrors `feature-expense`'s [CategorySheet] for visual consistency.
 *
 * @param categories Available categories to display.
 * @param selectedId The currently selected category id, or null.
 * @param onDismiss Callback when the sheet is dismissed.
 * @param onCategorySelected Callback when a category chip is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringCategorySheet(
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
                items = categories,
                key = { it.id },
            ) { category ->
                CategoryChip(
                    name = category.name,
                    color = Color(category.color),
                    icon = category.icon?.let { iconNameToImageVector(it) },
                    selected = category.id == selectedId,
                    onClick = { onCategorySelected(category.id) },
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    color: Color,
    icon: ImageVector?,
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
            modifier = Modifier.size(width = 120.dp, height = 72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
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
}
