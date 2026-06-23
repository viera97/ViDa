package com.vida.feature.expense.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Date/time selector card with a [DatePickerDialog].
 *
 * Displays the current date/time formatted as "dd/MM/yyyy HH:mm". Tapping
 * the card opens a Material 3 [DatePickerDialog]. The selected date
 * replaces the date portion of the current [dateTime], preserving the time.
 *
 * @param dateTime Current date/time instant.
 * @param onChanged Callback when a new date is confirmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelector(
    dateTime: Instant,
    onChanged: (Instant) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }

    val displayText = remember(dateTime) {
        dateTime.atZone(zone).format(formatter)
    }

    OutlinedCard(
        onClick = { showDatePicker = true },
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp),
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toEpochMilli(),
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(zone)
                                .toLocalDate()
                            val currentTime = dateTime.atZone(zone).toLocalTime()
                            val newDateTime = selectedDate
                                .atTime(currentTime)
                                .atZone(zone)
                                .toInstant()
                            onChanged(newDateTime)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
