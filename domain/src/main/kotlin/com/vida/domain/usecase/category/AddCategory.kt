package com.vida.domain.usecase.category

import com.vida.domain.model.Category
import com.vida.domain.repository.CategoryRepository

/**
 * Persists a new user-created category. The `isSystem` flag on the incoming [category]
 * is forced to `false` — only [com.vida.domain.model.DefaultCategories] may set it.
 *
 * @return the row id assigned by the persistence layer.
 */
class AddCategory(private val repo: CategoryRepository) {
    suspend operator fun invoke(category: Category): Long {
        require(category.name.isNotBlank()) { "Category name must not be blank" }
        return repo.upsert(category.copy(isSystem = false))
    }
}