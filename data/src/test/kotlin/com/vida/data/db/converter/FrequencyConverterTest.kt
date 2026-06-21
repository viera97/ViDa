package com.vida.data.db.converter

import com.vida.domain.model.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FrequencyConverterTest {

    private val converter = FrequencyConverter

    @Test
    fun `round-trips DAILY`() {
        assertEquals("DAILY", converter.fromFrequency(Frequency.DAILY))
        assertEquals(Frequency.DAILY, converter.toFrequency("DAILY"))
    }

    @Test
    fun `round-trips WEEKLY`() {
        assertEquals("WEEKLY", converter.fromFrequency(Frequency.WEEKLY))
        assertEquals(Frequency.WEEKLY, converter.toFrequency("WEEKLY"))
    }

    @Test
    fun `round-trips MONTHLY`() {
        assertEquals("MONTHLY", converter.fromFrequency(Frequency.MONTHLY))
        assertEquals(Frequency.MONTHLY, converter.toFrequency("MONTHLY"))
    }

    @Test
    fun `round-trips YEARLY`() {
        assertEquals("YEARLY", converter.fromFrequency(Frequency.YEARLY))
        assertEquals(Frequency.YEARLY, converter.toFrequency("YEARLY"))
    }

    @Test
    fun `throws on unknown enum name`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.toFrequency("UNKNOWN")
        }
    }
}
