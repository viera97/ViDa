package com.vida.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletTest {

    @Test
    fun `default name is Billetera`() {
        val wallet = Wallet(id = 1L, currency = "CUP")
        assertEquals("Billetera", wallet.name)
    }

    @Test
    fun `explicit name overrides default`() {
        val wallet = Wallet(id = 1L, currency = "CUP", name = "Mi Billetera")
        assertEquals("Mi Billetera", wallet.name)
    }

    @Test
    fun `wallet equality includes name`() {
        val a = Wallet(id = 1L, currency = "CUP", name = "A")
        val b = Wallet(id = 1L, currency = "CUP", name = "A")
        val c = Wallet(id = 1L, currency = "CUP", name = "B")
        assertEquals(a, b)
        assertEquals("B", c.name)
        assertEquals("CUP", a.currency)
    }
}
