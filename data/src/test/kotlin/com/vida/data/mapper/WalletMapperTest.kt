package com.vida.data.mapper

import com.vida.domain.model.Currency
import com.vida.domain.model.Wallet
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletMapperTest {
    private val mapper = WalletMapper

    @Test
    fun `round trip preserves all fields`() {
        val wallet = Wallet(currency = Currency.CUP)
        val entity = mapper.toEntity(wallet)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(wallet, roundTrip)
    }

    @Test
    fun `all currencies round trip`() {
        for (currency in Currency.values()) {
            val wallet = Wallet(currency = currency)
            val entity = mapper.toEntity(wallet)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(wallet, roundTrip)
        }
    }

    @Test
    fun `id 1 is preserved across round trip`() {
        val wallet = Wallet(currency = Currency.MLC)
        val entity = mapper.toEntity(wallet)
        assertEquals(1L, entity.id)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(1L, roundTrip.id)
    }

    @Test
    fun `name maps entity to domain`() {
        val entity = mapper.toEntity(Wallet(currency = Currency.CUP))
        entity.copy(name = "Mi Billetera").let { modified ->
            val wallet = mapper.toDomain(modified)
            assertEquals("Mi Billetera", wallet.name)
        }
    }

    @Test
    fun `name maps domain to entity`() {
        val wallet = Wallet(currency = Currency.USD, name = "Billetera USD")
        val entity = mapper.toEntity(wallet)
        assertEquals("Billetera USD", entity.name)
    }
}
