package com.vida.domain.usecase.category

import com.vida.domain.model.Category
import com.vida.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SeedDefaultCategoriesTest {

    @Test
    fun `seeds all six default categories on empty repo`() = runTest {
        val repo = mockk<CategoryRepository>(relaxed = true)
        coEvery { repo.getAll() } returns flowOf(emptyList())
        coEvery { repo.upsert(any()) } returns 0L

        SeedDefaultCategories(repo).invoke()

        // 6 system categories seeded (FOOD, TRANSPORT, HOUSING, HEALTH, ENTERTAINMENT, OTHER)
        coVerify(exactly = 6) { repo.upsert(any()) }
    }

    @Test
    fun `is a no-op when categories already exist`() = runTest {
        val repo = mockk<CategoryRepository>(relaxed = true)
        val existing = Category(
            id = 1L,
            name = "Comida",
            color = 0,
            icon = null,
            isSystem = true,
        )
        coEvery { repo.getAll() } returns flowOf(listOf(existing))

        SeedDefaultCategories(repo).invoke()

        coVerify(exactly = 0) { repo.upsert(any()) }
    }
}