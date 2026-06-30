package com.vida.feature.stashmanagement

/**
 * UI state for the stash list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (after mutation + refetch)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface StashListUiState {

    /** Emitted while [com.vida.domain.usecase.stash.ListStashes] is in-flight. */
    data object Loading : StashListUiState

    /** Stashes loaded and sorted (by name). */
    data class Ready(val items: List<StashDisplayItem>) : StashListUiState

    /** No stashes exist in the database. */
    data object Empty : StashListUiState

    /** Initial load failed. Retry available via [StashListViewModel.onDismissError]. */
    data class Error(val message: String) : StashListUiState
}

/**
 * Pre-formatted display item for a single stash row.
 *
 * @property id Stash row id.
 * @property name Stash name.
 * @property currencyCode Currency code string ("CUP", "USD", "MLC") for badge rendering.
 * @property createdAtFormatted Creation date as "dd/MM/yyyy".
 * @property updatedAtFormatted Last update date as "dd/MM/yyyy".
 */
data class StashDisplayItem(
    val id: Long,
    val name: String,
    val currencyCode: String,
    val createdAtFormatted: String,
    val updatedAtFormatted: String,
)

/**
 * One-shot navigation events emitted by [StashListViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class StashNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : StashNavEvent()

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : StashNavEvent()
}
