package com.vida.data.db.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class InstantConverterTest {

    private val converter = InstantConverter

    @Test
    fun `round-trips current instant`() {
        val now = Instant.now()
        val millis = converter.fromInstant(now)
        val restored = converter.toInstant(millis)
        assertEquals(now.toEpochMilli(), restored!!.toEpochMilli())
    }

    @Test
    fun `round-trips epoch zero`() {
        val epoch = Instant.EPOCH
        val millis = converter.fromInstant(epoch)
        assertEquals(0L, millis)
        assertEquals(epoch, converter.toInstant(millis))
    }

    @Test
    fun `round-trips far future`() {
        val future = Instant.parse("2099-12-31T23:59:59Z")
        val millis = converter.fromInstant(future)
        assertEquals(future, converter.toInstant(millis))
    }

    @Test
    fun `handles null input`() {
        assertNull(converter.fromInstant(null))
        assertNull(converter.toInstant(null))
    }
}
