package com.vida.data.mapper

import com.vida.data.db.entity.CurrencyEntity
import com.vida.domain.model.CurrencyInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyMapperTest {
    private val mapper = CurrencyMapper

    @Test
    fun `round trip preserves all fields with isSystem true`() {
        val currency = CurrencyInfo(
            id = 1L,
            name = "Dólar",
            code = "USD",
            isSystem = true,
        )
        val entity = mapper.toEntity(currency)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(currency, roundTrip)
        assertEquals(1, entity.isSystem)
    }

    @Test
    fun `round trip preserves all fields with isSystem false`() {
        val currency = CurrencyInfo(
            id = 2L,
            name = "Bitcoin",
            code = "BTC",
            isSystem = false,
        )
        val entity = mapper.toEntity(currency)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(currency, roundTrip)
        assertEquals(0, entity.isSystem)
    }

    @Test
    fun `toEntity maps isSystem boolean to integer 0`() {
        val entity = mapper.toEntity(
            CurrencyInfo(id = 0L, name = "User Cur", code = "UCU", isSystem = false),
        )
        assertEquals(0, entity.isSystem)
    }

    @Test
    fun `toEntity maps isSystem boolean to integer 1`() {
        val entity = mapper.toEntity(
            CurrencyInfo(id = 0L, name = "Sys Cur", code = "SCU", isSystem = true),
        )
        assertEquals(1, entity.isSystem)
    }

    @Test
    fun `toDomain maps integer 0 to false and 1 to true`() {
        val falseCur = mapper.toDomain(
            CurrencyEntity(id = 1L, name = "A", code = "AAA", isSystem = 0),
        )
        assertEquals(false, falseCur.isSystem)

        val trueCur = mapper.toDomain(
            CurrencyEntity(id = 2L, name = "B", code = "BBB", isSystem = 1),
        )
        assertEquals(true, trueCur.isSystem)
    }

    @Test
    fun `round trip preserves code and name exactly`() {
        val currency = CurrencyInfo(
            id = 7L,
            name = "Peso cubano",
            code = "CUP",
            isSystem = true,
        )
        val entity = mapper.toEntity(currency)
        assertEquals("Peso cubano", entity.name)
        assertEquals("CUP", entity.code)

        val roundTrip = mapper.toDomain(entity)
        assertEquals(currency, roundTrip)
    }
}
