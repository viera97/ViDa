package com.vida.domain.usecase.category

import com.vida.domain.model.DefaultCategories
import com.vida.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first

/**
 * Seeds the system categories from [DefaultCategories] on first run.
 *
 * Idempotent: if any row already exists in the table the invocation is a no-op, so the
 * `:data` layer may call this from Room's `onCreate` without tracking whether it ran.
 *
 * Out of scope for `:domain`: triggering the seed. `:data` is responsible for wiring
 * this to `RoomDatabase.Callback.onCreate` (or similar).
 */
class SeedDefaultCategories(private val repo: CategoryRepository) {
    suspend operator fun invoke() {
        val existing = repo.getAll().first()
        if (existing.isNotEmpty()) return
        DefaultCategories.ALL.forEach { category -> repo.upsert(category) }
    }
}