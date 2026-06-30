package com.vida.data.mapper

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletMapperTest {
    private val mapper = WalletMapper

    @Test
    fun `round trip preserves all fields`() {
        val wallet = Wallet(currency = Currency.CUP, balance = Money.of("0.00", Currency.CUP))
        val entity = mapper.toEntity(wallet)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(wallet, roundTrip)
    }

    @Test
    fun `all currencies round trip`() {
        for (currency in Currency.values()) {
            val wallet = Wallet(currency = currency, balance = Money.of("0.00", currency))
            val entity = mapper.toEntity(wallet)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(wallet, roundTrip)
        }
    }

    @Test
    fun `id is preserved across round trip`() {
        val wallet = Wallet(id = 7L, currency = Currency.MLC)
        val entity = mapper.toEntity(wallet)
        assertEquals(7L, entity.id)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(7L, roundTrip.id)
    }

    @Test
    fun `name maps entity to domain`() {
        val entity = mapper.toEntity(Wallet(currency = Currency.CUP, balance = Money.of("0.00", Currency.CUP)))
        entity.copy(name = "Mi Billetera").let { modified ->
            val wallet = mapper.toDomain(modified)
            assertEquals("Mi Billetera", wallet.name)
        }
    }

    @Test
    fun `name maps domain to entity`() {
        val wallet = Wallet(currency = Currency.USD, name = "Billetera USD", balance = Money.of("0.00", Currency.USD))
        val entity = mapper.toEntity(wallet)
        assertEquals("Billetera USD", entity.name)
    }
}
