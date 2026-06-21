package com.vida.data.mapper

import com.vida.domain.model.Currency
import com.vida.domain.model.Stash
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class StashMapperTest {
    private val mapper = StashMapper

    @Test
    fun `round trip preserves all fields`() {
        val now = Instant.now()
        val stash = Stash(
            name = "Vacation Fund",
            createdAt = now,
            updatedAt = now,
            currency = Currency.USD,
        )
        val entity = mapper.toEntity(stash)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(stash, roundTrip)
    }

    @Test
    fun `all currencies round trip`() {
        val now = Instant.now()
        for (currency in Currency.values()) {
            val stash = Stash(
                name = "Stash ${currency.code}",
                createdAt = now,
                updatedAt = now,
                currency = currency,
            )
            val entity = mapper.toEntity(stash)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(stash, roundTrip)
        }
    }

    @Test
    fun `epoch boundary timestamps round trip`() {
        val epoch = Instant.ofEpochMilli(0)
        val stash = Stash(
            name = "Epoch Stash",
            createdAt = epoch,
            updatedAt = epoch,
            currency = Currency.CUP,
        )
        val entity = mapper.toEntity(stash)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(stash, roundTrip)
    }
}
