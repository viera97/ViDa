package com.vida.data.mapper

import com.vida.data.db.entity.CategoryEntity
import com.vida.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMapperTest {
    private val mapper = CategoryMapper

    @Test
    fun `round trip preserves all fields with isSystem true`() {
        val category = Category(
            id = 1L,
            name = "Comida",
            color = 0xFFE57373.toInt(),
            icon = "restaurant",
            isSystem = true,
        )
        val entity = mapper.toEntity(category)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(category, roundTrip)
        assertEquals(1, entity.isSystem)
    }

    @Test
    fun `round trip preserves all fields with isSystem false`() {
        val category = Category(
            id = 2L,
            name = "Transporte",
            color = 0xFF42A5F5.toInt(),
            icon = null,
            isSystem = false,
        )
        val entity = mapper.toEntity(category)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(category, roundTrip)
        assertEquals(0, entity.isSystem)
    }

    @Test
    fun `toEntity maps isSystem boolean to integer 0`() {
        val entity = mapper.toEntity(
            Category(id = 0L, name = "User Cat", color = 0, isSystem = false),
        )
        assertEquals(0, entity.isSystem)
    }

    @Test
    fun `toEntity maps isSystem boolean to integer 1`() {
        val entity = mapper.toEntity(
            Category(id = 0L, name = "Sys Cat", color = 0, isSystem = true),
        )
        assertEquals(1, entity.isSystem)
    }

    @Test
    fun `toDomain maps integer 0 to false and 1 to true`() {
        val falseCat = mapper.toDomain(CategoryEntity(id = 1L, name = "A", color = 0, icon = null, isSystem = 0))
        assertEquals(false, falseCat.isSystem)
        val trueCat = mapper.toDomain(CategoryEntity(id = 2L, name = "B", color = 0, icon = null, isSystem = 1))
        assertEquals(true, trueCat.isSystem)
    }
}
