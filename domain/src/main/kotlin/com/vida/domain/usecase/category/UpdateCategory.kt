package com.vida.domain.usecase.category

import com.vida.domain.model.Category
import com.vida.domain.repository.CategoryRepository

/**
 * Updates an existing category. The id MUST be > 0 (a fresh id means insert, not update).
 * Editing a system category's display attributes is allowed; only deletion is blocked.
 *
 * @return the row id assigned by the persistence layer.
 */
class UpdateCategory(private val repo: CategoryRepository) {
    suspend operator fun invoke(category: Category): Long {
        require(category.id > 0L) { "Category id must be > 0 to update" }
        require(category.name.isNotBlank()) { "Category name must not be blank" }
        return repo.upsert(category)
    }
}