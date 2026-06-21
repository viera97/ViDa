package com.vida.data.db

import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Frequency
import com.vida.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {
    private val converters = Converters()

    // ── Currency ───────────────────────────────────────────────────────

    @Test
    fun `currency round trip for all values`() {
        for (currency in Currency.values()) {
            val text = converters.fromCurrency(currency)
            val back = converters.toCurrency(text)
            assertEquals(currency, back)
        }
    }

    @Test
    fun `currency from unknown code throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.toCurrency("XYZ")
        }
    }

    // ── Instant ────────────────────────────────────────────────────────

    @Test
    fun `instant round trip`() {
        val now = Instant.now()
        val millis = converters.fromInstant(now)
        val back = converters.toInstant(millis)
        assertEquals(now.toEpochMilli(), back.toEpochMilli())
    }

    @Test
    fun `instant epoch zero round trip`() {
        val epoch = Instant.ofEpochMilli(0)
        val millis = converters.fromInstant(epoch)
        val back = converters.toInstant(millis)
        assertEquals(epoch, back)
    }

    // ── LocalDate ──────────────────────────────────────────────────────

    @Test
    fun `local date round trip`() {
        val date = LocalDate.now()
        val days = converters.fromLocalDate(date)
        val back = converters.toLocalDate(days)
        assertEquals(date, back)
    }

    @Test
    fun `local date epoch zero round trip`() {
        val epoch = LocalDate.ofEpochDay(0)
        val days = converters.fromLocalDate(epoch)
        val back = converters.toLocalDate(days)
        assertEquals(epoch, back)
    }

    // ── CardType ───────────────────────────────────────────────────────

    @Test
    fun `card type round trip for all values`() {
        for (type in CardType.values()) {
            val text = converters.fromCardType(type)
            val back = converters.toCardType(text)
            assertEquals(type, back)
        }
    }

    @Test
    fun `card type unknown throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.toCardType("UNKNOWN")
        }
    }

    // ── SourceType ─────────────────────────────────────────────────────

    @Test
    fun `source type round trip for all values`() {
        for (type in SourceType.values()) {
            val text = converters.fromSourceType(type)
            val back = converters.toSourceType(text)
            assertEquals(type, back)
        }
    }

    @Test
    fun `source type unknown throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.toSourceType("UNKNOWN")
        }
    }

    // ── Frequency ─────────────────────────────────────────────────────

    @Test
    fun `frequency round trip for all values`() {
        for (frequency in Frequency.values()) {
            val text = converters.fromFrequency(frequency)
            val back = converters.toFrequency(text)
            assertEquals(frequency, back)
        }
    }

    @Test
    fun `frequency unknown throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            converters.toFrequency("UNKNOWN")
        }
    }
}
