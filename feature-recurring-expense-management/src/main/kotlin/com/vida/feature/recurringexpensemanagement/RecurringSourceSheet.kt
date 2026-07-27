package com.vida.feature.recurringexpensemanagement

import androidx.compose.runtime.Composable
import com.vida.core.ui.SourceItem
import com.vida.core.ui.SourceSheet

/**
 * Re-exports the shared grid-style [SourceSheet] under the legacy name
 * [RecurringSourceSheet] for backward compatibility.
 *
 * The old list-style layout has been replaced by the grid layout used
 * in the main expense form for visual consistency.
 */
@Composable
fun RecurringSourceSheet(
    sources: List<SourceItem>,
    selectedSource: SourceItem?,
    onDismiss: () -> Unit,
    onSourceSelected: (SourceItem) -> Unit,
) {
    SourceSheet(
        sources = sources,
        selectedSource = selectedSource,
        onDismiss = onDismiss,
        onSourceSelected = onSourceSelected,
    )
}
