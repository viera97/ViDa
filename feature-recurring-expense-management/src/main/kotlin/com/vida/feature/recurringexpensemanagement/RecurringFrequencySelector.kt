package com.vida.feature.recurringexpensemanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vida.domain.model.Frequency

/**
 * Trigger field for the recurring expense frequency picker bottom sheet.
 *
 * Renders as a disabled [OutlinedTextField] (transparent background, matching
 * the rest of the recurring form fields) wrapped in a clickable [Box]. Tapping
 * the field invokes [onShowSheet].
 *
 * Displays the Spanish label for the currently selected [Frequency].
 *
 * @param selectedFrequency The currently selected frequency.
 * @param onShowSheet Callback to open the frequency bottom sheet.
 */
@Composable
fun RecurringFrequencySelector(
    selectedFrequency: Frequency,
    onShowSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().clickable(onClick = onShowSheet)) {
        OutlinedTextField(
            value = selectedFrequency.toSpanishLabel(),
            onValueChange = {},
            label = { Text("Frecuencia") },
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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

/** Spanish label for [Frequency] enum values. */
internal fun Frequency.toSpanishLabel(): String = when (this) {
    Frequency.DAILY -> "Diario"
    Frequency.WEEKLY -> "Semanal"
    Frequency.MONTHLY -> "Mensual"
    Frequency.YEARLY -> "Anual"
}
