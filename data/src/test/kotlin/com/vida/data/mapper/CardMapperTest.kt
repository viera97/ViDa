package com.vida.data.mapper

import com.vida.data.db.entity.CardEntity
import com.vida.domain.model.Card
import com.vida.domain.model.CardNumber
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CardMapperTest {
    private val mapper = CardMapper

    @Test
    fun `round trip preserves all fields`() {
        val card = Card(
            number = CardNumber.fromFirst6Last4("123456", "7890"),
            bank = "Banco Popular",
            type = CardType.DEBIT,
            currency = Currency.CUP,
            note = "Main card",
            expirationDate = LocalDate.of(2028, 12, 31),
        )
        val entity = mapper.toEntity(card)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(card, roundTrip)
    }

    @Test
    fun `null note maps correctly`() {
        val card = Card(
            number = CardNumber.fromFirst6Last4("654321", "4321"),
            bank = "Cash",
            type = CardType.CREDIT,
            currency = Currency.USD,
            note = null,
            expirationDate = LocalDate.of(2029, 6, 15),
        )
        val entity = mapper.toEntity(card)
        val roundTrip = mapper.toDomain(entity)
        assertEquals(card, roundTrip)
    }

    @Test
    fun `all card types round trip`() {
        for (type in CardType.values()) {
            val card = Card(
                number = CardNumber.fromFirst6Last4("111111", "2222"),
                bank = "Test Bank",
                type = type,
                currency = Currency.MLC,
                note = null,
                expirationDate = LocalDate.of(2030, 1, 1),
            )
            val entity = mapper.toEntity(card)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(card, roundTrip)
        }
    }

    @Test
    fun `all currencies round trip`() {
        for (currency in Currency.values()) {
            val card = Card(
                number = CardNumber.fromFirst6Last4("999999", "8888"),
                bank = "MultiCurrency Bank",
                type = CardType.PREPAID,
                currency = currency,
                note = "Holds $currency",
                expirationDate = LocalDate.of(2027, 3, 15),
            )
            val entity = mapper.toEntity(card)
            val roundTrip = mapper.toDomain(entity)
            assertEquals(card, roundTrip)
        }
    }
}
