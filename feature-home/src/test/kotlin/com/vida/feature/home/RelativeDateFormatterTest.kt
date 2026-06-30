package com.vida.feature.home

import com.vida.core.format.toRelativeDateString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Unit tests for [com.vida.feature.home.util.toRelativeDateString].
 * Plain JUnit 4, pure JVM — covers all format branches (SCN-HOME-031..033 + edge cases).
 */
class RelativeDateFormatterTest {

    private val fixedNow: Instant = Instant.parse("2026-06-21T12:00:00Z")
    private val zone: ZoneId = ZoneId.of("America/Havana")

    private fun String.toRelative(): String =
        toRelativeDateString(now = fixedNow, zone = zone)

    // SCN-HOME-033 — "hace 3 horas"
    @Test
    fun `SCN-HOME-033 — 3 hours ago renders hace 3 horas`() {
        val threeHoursAgo = fixedNow.minus(Duration.ofHours(3))
        assertEquals("hace 3 horas", threeHoursAgo.toString().toRelative())
    }

    // SCN-HOME-032 — "ayer"
    @Test
    fun `SCN-HOME-032 — 1 day ago renders ayer`() {
        val yesterday = fixedNow.minus(Duration.ofDays(1))
        assertEquals("ayer", yesterday.toString().toRelative())
    }

    // SCN-HOME-031 — "hace 2 días"
    @Test
    fun `SCN-HOME-031 — 2 days ago renders hace 2 días`() {
        val twoDaysAgo = fixedNow.minus(Duration.ofDays(2))
        assertEquals("hace 2 días", twoDaysAgo.toString().toRelative())
    }

    // Edge: exactly 1 minute → "hace 1 minuto"
    @Test
    fun `exactly 1 minute ago renders singular minuto`() {
        val oneMinuteAgo = fixedNow.minus(Duration.ofMinutes(1))
        assertEquals("hace 1 minuto", oneMinuteAgo.toString().toRelative())
    }

    // Edge: exactly 1 hour → "hace 1 hora"
    @Test
    fun `exactly 1 hour ago renders singular hora`() {
        val oneHourAgo = fixedNow.minus(Duration.ofHours(1))
        assertEquals("hace 1 hora", oneHourAgo.toString().toRelative())
    }

    // Edge: 59 minutes → "hace 59 minutos"
    @Test
    fun `59 minutes ago renders plural minutos`() {
        val fiftyNineMinutesAgo = fixedNow.minus(Duration.ofMinutes(59))
        assertEquals("hace 59 minutos", fiftyNineMinutesAgo.toString().toRelative())
    }

    // Edge: 0 seconds → "ahora mismo"
    @Test
    fun `0 seconds ago renders ahora mismo`() {
        assertEquals("ahora mismo", fixedNow.toString().toRelative())
    }

    // Edge: > 7 days → absolute date fallback
    @Test
    fun `more than 7 days ago renders absolute date`() {
        val eightDaysAgo = fixedNow.minus(Duration.ofDays(8))
        assertEquals("13 jun 2026", eightDaysAgo.toString().toRelative())
    }

    // Edge: exactly 7 days → "hace 7 días"
    @Test
    fun `exactly 7 days ago renders hace 7 dias`() {
        val sevenDaysAgo = fixedNow.minus(Duration.ofDays(7))
        assertEquals("hace 7 días", sevenDaysAgo.toString().toRelative())
    }

    // Edge: invalid ISO string → fallback
    @Test
    fun `invalid ISO string renders fallback text`() {
        assertEquals("fecha inválida", "not-a-date".toRelative())
    }
}