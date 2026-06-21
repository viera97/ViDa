package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.CategoryDao
import com.vida.data.db.entity.CategoryEntity
import com.vida.data.mapper.CategoryMapper
import com.vida.domain.model.Category
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CategoryRepositoryImplTest {
    private val dao = mockk<CategoryDao>(relaxed = true)
    private val mapper = CategoryMapper
    private lateinit var repository: CategoryRepositoryImpl

    @Before
    fun setUp() {
        repository = CategoryRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = CategoryEntity(id = 1L, name = "Comida", color = 0, icon = null, isSystem = 1)
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val categories = awaitItem()
            assertEquals(1, categories.size)
            assertEquals("Comida", categories[0].name)
            assertEquals(true, categories[0].isSystem)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped category when found`() = runTest {
        val entity = CategoryEntity(id = 1L, name = "Comida", color = 0, icon = null, isSystem = 0)
        coEvery { dao.getById(1L) } returns entity

        val category = repository.getById(1L)
        assertEquals("Comida", category!!.name)
        assertEquals(false, category.isSystem)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null

        assertNull(repository.getById(42L))
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val category = Category(id = 0L, name = "Comida", color = 0, isSystem = false)
        val entity = mapper.toEntity(category)
        coEvery { dao.upsert(entity) } returns 7L

        val id = repository.upsert(category)
        assertEquals(7L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }
}
