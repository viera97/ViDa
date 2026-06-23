package com.vida.feature.expense.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vida.domain.model.Category

/**
 * Trigger card for the category picker bottom sheet.
 *
 * Shows a colored dot + category name when a category is selected, or a
 * placeholder label otherwise. Tapping the card invokes [onShowSheet].
 *
 * @param selectedCategory The currently selected category, or null.
 * @param onShowSheet Callback to open the category bottom sheet.
 * @param error Validation error message from `validationErrors["category"]`, or null.
 */
@Composable
fun CategorySelector(
    selectedCategory: Category?,
    onShowSheet: () -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onShowSheet,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            if (selectedCategory != null) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(selectedCategory.color)),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedCategory.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
        }
    }
}
