package com.vida.data.repository

import app.cash.turbine.test
import com.vida.data.db.dao.CardDao
import com.vida.data.db.entity.CardEntity
import com.vida.data.mapper.CardMapper
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CardRepositoryImplTest {
    private val dao = mockk<CardDao>(relaxed = true)
    private val mapper = CardMapper
    private lateinit var repository: CardRepositoryImpl

    @Before
    fun setUp() {
        repository = CardRepositoryImpl(dao, mapper)
    }

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        val entity = aCardEntity()
        coEvery { dao.observeAll() } returns flowOf(listOf(entity))

        repository.getAll().test {
            val cards = awaitItem()
            assertEquals(1, cards.size)
            assertEquals("POP", cards[0].bank)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns mapped card when found`() = runTest {
        val entity = aCardEntity()
        coEvery { dao.getById(1L) } returns entity

        val card = repository.getById(1L)
        assertEquals("POP", card!!.bank)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { dao.getById(any()) } returns null

        val result = repository.getById(42L)
        assertNull(result)
    }

    @Test
    fun `upsert delegates to dao and returns id`() = runTest {
        val card = aCard()
        val entity = mapper.toEntity(card)
        coEvery { dao.upsert(entity) } returns 42L

        val id = repository.upsert(card)
        assertEquals(42L, id)
        coVerify { dao.upsert(entity) }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(1L)
        coVerify { dao.delete(1L) }
    }

    @Test
    fun `getBalance throws NotImplementedError`() = runTest {
        try {
            repository.getBalance(1L)
            assert(false) { "Expected NotImplementedError" }
        } catch (_: NotImplementedError) {
            // Expected
        }
    }

    private fun aCardEntity() = CardEntity(
        maskedNumber = "123456******7890",
        bank = "POP",
        type = CardType.DEBIT,
        currency = Currency.CUP,
        note = null,
        expirationDate = LocalDate.of(2028, 12, 31),
    )

    private fun aCard() = Card(
        number = CardNumber.fromFirst6Last4("123456", "7890"),
        bank = "POP",
        type = CardType.DEBIT,
        currency = Currency.CUP,
        note = null,
        expirationDate = LocalDate.of(2028, 12, 31),
    )
}
