package com.vida.data.db.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LocalDateConverterTest {

    private val converter = LocalDateConverter

    @Test
    fun `round-trips today`() {
        val today = LocalDate.now()
        val epochDay = converter.fromLocalDate(today)
        assertEquals(today, converter.toLocalDate(epochDay))
    }

    @Test
    fun `round-trips epoch zero`() {
        val epoch = LocalDate.ofEpochDay(0)
        val epochDay = converter.fromLocalDate(epoch)
        assertEquals(0L, epochDay)
        assertEquals(epoch, converter.toLocalDate(epochDay))
    }

    @Test
    fun `round-trips leap date`() {
        val leap = LocalDate.of(2024, 2, 29)
        val epochDay = converter.fromLocalDate(leap)
        assertEquals(leap, converter.toLocalDate(epochDay))
    }

    @Test
    fun `handles null input`() {
        assertNull(converter.fromLocalDate(null))
        assertNull(converter.toLocalDate(null))
    }
}
