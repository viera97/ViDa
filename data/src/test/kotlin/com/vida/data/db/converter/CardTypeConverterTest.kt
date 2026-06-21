package com.vida.data.db.converter

import com.vida.domain.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CardTypeConverterTest {

    private val converter = CardTypeConverter

    @Test
    fun `round-trips DEBIT`() {
        assertEquals("DEBIT", converter.fromCardType(CardType.DEBIT))
        assertEquals(CardType.DEBIT, converter.toCardType("DEBIT"))
    }

    @Test
    fun `round-trips CREDIT`() {
        assertEquals("CREDIT", converter.fromCardType(CardType.CREDIT))
        assertEquals(CardType.CREDIT, converter.toCardType("CREDIT"))
    }

    @Test
    fun `round-trips PREPAID`() {
        assertEquals("PREPAID", converter.fromCardType(CardType.PREPAID))
        assertEquals(CardType.PREPAID, converter.toCardType("PREPAID"))
    }

    @Test
    fun `throws on unknown enum name`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.toCardType("UNKNOWN")
        }
    }
}
