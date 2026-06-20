package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CardNumberTest {

    @Test
    fun `fromFull masks middle six digits of a 16-digit number`() {
        val cn = CardNumber.fromFull("1234567890123456")
        assertEquals("123456******3456", cn.masked)
    }

    @Test
    fun `fromFull rejects wrong length`() {
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFull("1234567890")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFull("12345678901234567")
        }
    }

    @Test
    fun `fromFull rejects non-digit characters`() {
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFull("123456789012345A")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFull("1234-67890123456")
        }
    }

    @Test
    fun `fromFirst6Last4 builds masked card number from components`() {
        val cn = CardNumber.fromFirst6Last4("123456", "7890")
        assertEquals("123456******7890", cn.masked)
    }

    @Test
    fun `fromFirst6Last4 rejects first6 of wrong length or with non-digits`() {
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFirst6Last4("12345", "7890")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFirst6Last4("12345A", "7890")
        }
    }

    @Test
    fun `fromFirst6Last4 rejects last4 of wrong length or with non-digits`() {
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFirst6Last4("123456", "789")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CardNumber.fromFirst6Last4("123456", "789X")
        }
    }
}