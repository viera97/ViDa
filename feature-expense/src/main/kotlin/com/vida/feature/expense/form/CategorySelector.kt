package com.vida.feature.expense.form

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vida.core.icon.iconNameToImageVector
import com.vida.domain.model.Category

/**
 * Trigger field for the category picker bottom sheet.
 *
 * Renders as a disabled [OutlinedTextField] (transparent background, matching
 * the rest of the expense form fields like [AmountSection] and
 * [DescriptionInput]) wrapped in a clickable [Box]. Tapping the field invokes
 * [onShowSheet].
 *
 * - When [selectedCategory] is non-null, displays a colored dot + icon + category name.
 * - Otherwise displays a "Categoría" placeholder.
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
    Box(modifier = modifier.fillMaxWidth().clickable(onClick = onShowSheet)) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "",
            onValueChange = {},
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCategory != null) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(selectedCategory.color)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = iconNameToImageVector(selectedCategory.icon),
                            contentDescription = null,
                            tint = Color(selectedCategory.color),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedCategory.name)
                    } else {
                        Text("Categoría")
                    }
                }
            },
            isError = error != null,
            supportingText = error?.let { msg -> { Text(msg) } },
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledSupportingTextColor = MaterialTheme.colorScheme.error,
            ),
        )
    }
}
