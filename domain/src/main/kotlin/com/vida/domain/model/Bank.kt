package com.vida.domain.model

/**
 * Bank entity available for card association.
 *
 * - **System banks** — seeded by [DefaultBanks]; `isSystem = true`. Their deletion
 *   is blocked by the UI. Their name/color MAY be edited.
 * - **User banks** — created via `AddBank`; `isSystem = false`. Fully editable.
 *
 * @property id row id (0 means unsaved)
 * @property name display name (1..50 chars, not blank)
 * @property color ARGB int used by the UI to tint card gradients
 * @property isSystem true iff this row came from [DefaultBanks]
 */
data class Bank(
    val id: Long = 0L,
    val name: String,
    val color: Int,
    val isSystem: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Bank name must not be blank" }
        require(name.length <= 50) { "Bank name must be ≤ 50 chars" }
    }
}
