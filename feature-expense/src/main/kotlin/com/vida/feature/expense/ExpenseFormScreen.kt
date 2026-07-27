package com.vida.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vida.domain.model.Category
import com.vida.feature.expense.form.AmountSection
import com.vida.feature.expense.form.CategorySelector
import com.vida.feature.expense.form.CategorySheet
import com.vida.feature.expense.form.DateSelector
import com.vida.feature.expense.form.DescriptionInput
import com.vida.feature.expense.form.NoteInput
import com.vida.feature.expense.form.SourceSelector
import com.vida.feature.expense.form.SourceSheet
import com.vida.feature.expense.form.TimeSelector
import java.time.Instant

/**
 * Root composable for the expense recording form.
 *
 * Observes [ExpenseFormViewModel.uiState] via [collectAsStateWithLifecycle] and
 * renders the appropriate screen state inside a [Scaffold]:
 * - [ExpenseFormUiState.Loading] → centered [CircularProgressIndicator]
 * - [ExpenseFormUiState.Ready] → scrollable form with all field sections + submit button
 * - [ExpenseFormUiState.Submitting] → last ready form content + dimmed overlay with progress
 * - [ExpenseFormUiState.Success] → navigates back via [LaunchedEffect]
 * - [ExpenseFormUiState.Error] → last ready form content + [SnackbarHost] message
 *
 * The last valid [ExpenseFormUiState.Ready] is cached so the form remains
 * visible during [ExpenseFormUiState.Submitting] and [ExpenseFormUiState.Error].
 *
 * Bottom sheets for category and source selection are managed by local
 * [mutableStateOf] flags and rendered at the end of the scaffold content.
 *
 * @param onNavigateBack Callback invoked to navigate back (toolbar arrow or on success).
 * @param viewModel The [ExpenseFormViewModel], injected via Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExpenseFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cache the last Ready state so the form is visible during Submitting/Error.
    var lastReady by remember { mutableStateOf<ExpenseFormUiState.Ready?>(null) }
    (uiState as? ExpenseFormUiState.Ready)?.let { lastReady = it }

    // Bottom sheet visibility flags.
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSourceSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo gasto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                is ExpenseFormUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ExpenseFormUiState.NoSources -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = "Sin fuentes",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay fuentes registradas. Para registrar un gasto primero agregá una billetera, tarjeta o ahorro desde la sección Fuentes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(onClick = onNavigateBack) {
                                Text("Volver")
                            }
                        }
                    }
                }

                is ExpenseFormUiState.Ready -> {
                    val ready = uiState as ExpenseFormUiState.Ready
                    val selectedCategory = ready.categories.find { it.id == ready.form.categoryId }
                    val selectedSource = ready.sources.find {
                        it.type == ready.form.sourceType && it.id == ready.form.sourceId
                    }
                    FormContent(
                        ready = ready,
                        selectedCategory = selectedCategory,
                        selectedSource = selectedSource,
                        isSubmitting = false,
                        onShowCategorySheet = { showCategorySheet = true },
                        onShowSourceSheet = { showSourceSheet = true },
                        onAmountChanged = viewModel::onAmountChanged,
                        onDescriptionChanged = viewModel::onDescriptionChanged,
                        onDateTimeChanged = viewModel::onDateTimeChanged,
                        onNoteChanged = viewModel::onNoteChanged,
                        onSubmit = viewModel::submit,
                    )
                }

                is ExpenseFormUiState.Submitting -> {
                    lastReady?.let { ready ->
                        val selectedCategory = ready.categories.find { it.id == ready.form.categoryId }
                        val selectedSource = ready.sources.find {
                            it.type == ready.form.sourceType && it.id == ready.form.sourceId
                        }
                        FormContent(
                            ready = ready,
                            selectedCategory = selectedCategory,
                            selectedSource = selectedSource,
                            isSubmitting = true,
                            onShowCategorySheet = { showCategorySheet = true },
                            onShowSourceSheet = { showSourceSheet = true },
                            onAmountChanged = viewModel::onAmountChanged,
                            onDescriptionChanged = viewModel::onDescriptionChanged,
                            onDateTimeChanged = viewModel::onDateTimeChanged,
                            onNoteChanged = viewModel::onNoteChanged,
                            onSubmit = viewModel::submit,
                        )
                    }
                    // Dimmed overlay with progress indicator.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ExpenseFormUiState.Success -> {
                    LaunchedEffect(Unit) {
                        onNavigateBack()
                    }
                }

                is ExpenseFormUiState.Error -> {
                    val error = uiState as ExpenseFormUiState.Error
                    lastReady?.let { ready ->
                        val selectedCategory = ready.categories.find { it.id == ready.form.categoryId }
                        val selectedSource = ready.sources.find {
                            it.type == ready.form.sourceType && it.id == ready.form.sourceId
                        }
                        FormContent(
                            ready = ready,
                            selectedCategory = selectedCategory,
                            selectedSource = selectedSource,
                            isSubmitting = false,
                            onShowCategorySheet = { showCategorySheet = true },
                            onShowSourceSheet = { showSourceSheet = true },
                            onAmountChanged = viewModel::onAmountChanged,
                            onDescriptionChanged = viewModel::onDescriptionChanged,
                            onDateTimeChanged = viewModel::onDateTimeChanged,
                            onNoteChanged = viewModel::onNoteChanged,
                            onSubmit = viewModel::submit,
                        )
                    }
                    LaunchedEffect(error.message) {
                        snackbarHostState.showSnackbar(error.message)
                    }
                }
            }

            // Category bottom sheet — rendered outside the when to overlay everything.
            if (showCategorySheet) {
                val ready = (uiState as? ExpenseFormUiState.Ready) ?: lastReady
                if (ready != null) {
                    CategorySheet(
                        categories = ready.categories,
                        selectedId = ready.form.categoryId,
                        onDismiss = { showCategorySheet = false },
                        onCategorySelected = { id ->
                            viewModel.onCategorySelected(id)
                            showCategorySheet = false
                        },
                    )
                }
            }

            // Source bottom sheet — rendered outside the when to overlay everything.
            if (showSourceSheet) {
                val ready = (uiState as? ExpenseFormUiState.Ready) ?: lastReady
                if (ready != null) {
                    SourceSheet(
                        sources = ready.sources,
                        selectedSource = ready.sources.find {
                            it.type == ready.form.sourceType && it.id == ready.form.sourceId
                        },
                        onDismiss = { showSourceSheet = false },
                        onSourceSelected = { source ->
                            viewModel.onSourceSelected(source.type, source.id)
                            showSourceSheet = false
                        },
                    )
                }
            }

            // Currency is auto-determined by the source — no standalone currency picker.
        }
    }
}

/**
 * Scrollable form content with all field sections and the submit button.
 *
 * This is extracted as a private composable to avoid duplication across
 * [ExpenseFormUiState.Ready], [ExpenseFormUiState.Submitting], and
 * [ExpenseFormUiState.Error] branches.
 */
@Composable
private fun FormContent(
    ready: ExpenseFormUiState.Ready,
    selectedCategory: Category?,
    selectedSource: SourceItem?,
    isSubmitting: Boolean,
    onShowCategorySheet: () -> Unit,
    onShowSourceSheet: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateTimeChanged: (Instant) -> Unit,
    onNoteChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        AmountSection(
            amount = ready.form.amount,
            amountError = ready.validationErrors["amount"],
            onAmountChanged = onAmountChanged,
        )
        Spacer(modifier = Modifier.height(12.dp))
        DescriptionInput(
            value = ready.form.description,
            error = ready.validationErrors["description"],
            onChanged = onDescriptionChanged,
        )
        Spacer(modifier = Modifier.height(12.dp))
        CategorySelector(
            selectedCategory = selectedCategory,
            onShowSheet = onShowCategorySheet,
            error = ready.validationErrors["category"],
        )
        Spacer(modifier = Modifier.height(12.dp))
        SourceSelector(
            selectedSource = selectedSource,
            onShowSheet = onShowSourceSheet,
            error = ready.validationErrors["source"],
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Date and time as separate fields side by side. Each emits its own
        // half; we combine them back into an Instant and forward to the VM.
        val zone = java.time.ZoneId.systemDefault()
        val currentDate = remember(ready.form.dateTime) {
            ready.form.dateTime.atZone(zone).toLocalDate()
        }
        val currentTime = remember(ready.form.dateTime) {
            ready.form.dateTime.atZone(zone).toLocalTime()
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateSelector(
                date = currentDate,
                onChanged = { newDate ->
                    onDateTimeChanged(newDate.atTime(currentTime).atZone(zone).toInstant())
                },
                modifier = Modifier.weight(1f),
            )
            TimeSelector(
                time = currentTime,
                onChanged = { newTime ->
                    onDateTimeChanged(currentDate.atTime(newTime).atZone(zone).toInstant())
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        NoteInput(
            value = ready.form.note,
            onChanged = onNoteChanged,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = !isSubmitting && ready.validationErrors.isEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Guardar gasto")
        }
    }
}
