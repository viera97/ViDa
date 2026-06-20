package com.vida.domain.model

/**
 * Spending category. Two flavors exist:
 *
 * - **System categories** — seeded by [DefaultCategories]; `isSystem = true`. Their deletion
 *   is blocked by `DeleteCategory`. Their name/color/icon MAY be edited via `UpdateCategory`
 *   (users can rename a default).
 * - **User categories** — created by `AddCategory`; `isSystem = false`. Fully editable.
 *
 * @property id row id (0 means unsaved)
 * @property name display name (1..50 chars, not blank)
 * @property color ARGB int used by the UI to tint chips
 * @property icon Material icon resource name (nullable — UI falls back to a generic glyph)
 * @property isSystem true iff this row came from [DefaultCategories]
 */
data class Category(
    val id: Long = 0L,
    val name: String,
    val color: Int,
    val icon: String? = null,
    val isSystem: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank" }
        require(name.length <= 50) { "Category name must be ≤ 50 chars" }
    }
}