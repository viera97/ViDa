package com.vida.feature.categorymanagement

/**
 * UI state for the category list screen.
 *
 * Transitions:
 * ```
 * Loading → Ready | Empty | Error
 * Ready   → Ready (after mutation + refetch)
 * Error   → Loading → ... (on retry)
 * ```
 */
sealed interface CategoryListUiState {

    /** Emitted while [com.vida.domain.usecase.category.ListCategories] is in-flight. */
    data object Loading : CategoryListUiState

    /** Categories loaded and sorted. */
    data class Ready(val categories: List<CategoryDisplayItem>) : CategoryListUiState

    /** No categories exist in the database. */
    data object Empty : CategoryListUiState

    /** Initial load failed. Retry available via [CategoryListViewModel.onDismissError]. */
    data class Error(val message: String) : CategoryListUiState
}

/**
 * Pre-formatted display item for a single category row.
 *
 * @property id Category row id.
 * @property name Display name.
 * @property color ARGB color int for the color dot.
 * @property isSystem Whether this is a seeded system category.
 * @property isSelectedForDelete Whether the delete confirmation is active (PR #2 wiring).
 */
data class CategoryDisplayItem(
    val id: Long,
    val name: String,
    val color: Int,
    val isSystem: Boolean,
    val isSelectedForDelete: Boolean = false,
)

/**
 * One-shot navigation events emitted by [CategoryListViewModel].
 *
 * Consumed via [kotlinx.coroutines.channels.Channel] and observed in the
 * composable with [androidx.compose.runtime.LaunchedEffect].
 */
sealed class CategoryNavEvent {
    /** Show a transient toast / snackbar message. */
    data class ShowToast(val message: String) : CategoryNavEvent()

    /** Emitted after an add or edit operation completes successfully. */
    data object SaveSuccess : CategoryNavEvent()
}
