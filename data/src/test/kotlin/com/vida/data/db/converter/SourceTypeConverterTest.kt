package com.vida.data.db.converter

import com.vida.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SourceTypeConverterTest {

    private val converter = SourceTypeConverter

    @Test
    fun `round-trips WALLET`() {
        assertEquals("WALLET", converter.fromSourceType(SourceType.WALLET))
        assertEquals(SourceType.WALLET, converter.toSourceType("WALLET"))
    }

    @Test
    fun `round-trips CARD`() {
        assertEquals("CARD", converter.fromSourceType(SourceType.CARD))
        assertEquals(SourceType.CARD, converter.toSourceType("CARD"))
    }

    @Test
    fun `round-trips STASH`() {
        assertEquals("STASH", converter.fromSourceType(SourceType.STASH))
        assertEquals(SourceType.STASH, converter.toSourceType("STASH"))
    }

    @Test
    fun `throws on unknown enum name`() {
        assertThrows(IllegalArgumentException::class.java) {
            converter.toSourceType("UNKNOWN")
        }
    }
}
