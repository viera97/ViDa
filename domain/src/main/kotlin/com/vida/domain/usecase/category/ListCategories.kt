package com.vida.domain.usecase.category

import com.vida.domain.model.Category
import com.vida.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full category list. Reactive: emits on every Room table change. */
class ListCategories(private val repo: CategoryRepository) {
    operator fun invoke(): Flow<List<Category>> = repo.getAll()
}