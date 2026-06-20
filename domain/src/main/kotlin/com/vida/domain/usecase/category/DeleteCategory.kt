package com.vida.domain.usecase.category

import com.vida.domain.repository.CategoryRepository

/**
 * Deletes a category by id. System categories (seeded by
 * [com.vida.domain.model.DefaultCategories]) are protected and cause this use case to
 * throw [IllegalStateException] — the UI must offer a "convert to user category"
 * affordance first if the user really wants the row gone.
 */
class DeleteCategory(private val repo: CategoryRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0L) { "Category id must be > 0" }
        val category = repo.getById(id)
            ?: throw NoSuchElementException("Category $id not found")
        require(!category.isSystem) { "Cannot delete a system category (id=$id)" }
        repo.delete(id)
    }
}