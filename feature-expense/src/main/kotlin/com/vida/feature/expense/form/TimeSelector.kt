package com.vida.feature.expense.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Time selector field that opens a [androidx.compose.material3.TimePickerDialog] on tap.
 *
 * Renders as a disabled [OutlinedTextField] (transparent background, matching
 * the rest of the expense form fields) wrapped in a clickable [Box]. The
 * [DateSelector] companion handles the date component; this one only emits
 * the picked [LocalTime]. Uses 24-hour format.
 *
 * @param time Currently displayed time.
 * @param onChanged Callback with the newly picked time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelector(
    time: LocalTime,
    onChanged: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val displayText = remember(time) {
        time.format(formatter)
    }

    Box(modifier = modifier.fillMaxWidth().clickable { showTimePicker = true }) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            label = { Text("Hora") },
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
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )

        androidx.compose.material3.TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Seleccionar hora") },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}