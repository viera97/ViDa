package com.vida.core.format

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats [this] ISO-8601 instant string as a Spanish relative date.
 *
 * Rules (S6):
 * - 0..59 seconds       → "ahora mismo"
 * - 1..59 minutes       → "hace X minuto(s)"
 * - 1..23 hours         → "hace X hora(s)"
 * - 1 day  (yesterday)  → "ayer"
 * - 2..7 days           → "hace X día(s)"
 * - > 7 days            → absolute fallback "dd MMM yyyy"
 *
 * @param now Reference instant (defaults to system clock).
 * @param zone Time zone for absolute date formatting.
 */
fun CharSequence.toRelativeDateString(
    now: Instant = Clock.systemDefaultZone().instant(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val then: Instant = runCatching { Instant.parse(toString()) }
        .getOrElse { return "fecha inválida" }
    val diff = Duration.between(then, now)

    return when {
        diff.toSeconds() < 60 -> "ahora mismo"
        diff.toMinutes() < 60 -> {
            val m = diff.toMinutes()
            "hace $m ${if (m == 1L) "minuto" else "minutos"}"
        }
        diff.toHours() < 24 -> {
            val h = diff.toHours()
            "hace $h ${if (h == 1L) "hora" else "horas"}"
        }
        diff.toDays() == 1L -> "ayer"
        diff.toDays() <= 7 -> {
            val d = diff.toDays()
            "hace $d ${if (d == 1L) "día" else "días"}"
        }
        else -> {
            val localDate = then.atZone(zone).toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "ES"))
            localDate.format(formatter)
        }
    }
}
