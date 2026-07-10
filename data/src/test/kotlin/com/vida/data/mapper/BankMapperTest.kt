package com.vida.data.mapper

import com.vida.data.db.entity.BankEntity
import com.vida.domain.model.Bank
import org.junit.Assert.assertEquals
import org.junit.Test

class BankMapperTest {
    private val mapper = BankMapper

    @Test
    fun `round trip preserves all fields with isSystem true`() {
        val bank = Bank(
            id = 1L,
            name = "Bandec",
            color = 0xFF8E0509.toInt(),
            isSystem = true,
        )
        val entity = mapper.toEntity(bank)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(bank, roundTrip)
        assertEquals(1, entity.isSystem)
    }

    @Test
    fun `round trip preserves all fields with isSystem false`() {
        val bank = Bank(
            id = 2L,
            name = "Mi Banco",
            color = 0xFF123456.toInt(),
            isSystem = false,
        )
        val entity = mapper.toEntity(bank)
        val roundTrip = mapper.toDomain(entity)

        assertEquals(bank, roundTrip)
        assertEquals(0, entity.isSystem)
    }

    @Test
    fun `toEntity maps isSystem boolean to integer 0`() {
        val entity = mapper.toEntity(
            Bank(id = 0L, name = "User Bank", color = 0, isSystem = false),
        )
        assertEquals(0, entity.isSystem)
    }

    @Test
    fun `toEntity maps isSystem boolean to integer 1`() {
        val entity = mapper.toEntity(
            Bank(id = 0L, name = "Sys Bank", color = 0, isSystem = true),
        )
        assertEquals(1, entity.isSystem)
    }

    @Test
    fun `toDomain maps integer 0 to false and 1 to true`() {
        val falseBank = mapper.toDomain(
            BankEntity(id = 1L, name = "A", color = 0, isSystem = 0),
        )
        assertEquals(false, falseBank.isSystem)

        val trueBank = mapper.toDomain(
            BankEntity(id = 2L, name = "B", color = 0, isSystem = 1),
        )
        assertEquals(true, trueBank.isSystem)
    }
}
