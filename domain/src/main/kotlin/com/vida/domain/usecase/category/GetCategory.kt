package com.vida.domain.usecase.category

import com.vida.domain.model.Category
import com.vida.domain.repository.CategoryRepository

/** Loads a single category by id, or returns null if it does not exist. */
class GetCategory(private val repo: CategoryRepository) {
    suspend operator fun invoke(id: Long): Category? = repo.getById(id)
}