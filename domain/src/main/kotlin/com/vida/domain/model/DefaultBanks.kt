package com.vida.domain.model

/**
 * Seed set of system banks inserted on first run via
 * `SeedDefaultBanks`. The id is the placeholder `0L`; the persistence layer
 * assigns the real row id on insert.
 *
 * Names are kept in Spanish to match the app UI (Q-locale decision from PR #1).
 */
object DefaultBanks {
    val BANDEC: Bank = Bank(
        id = 0L,
        name = "Bandec",
        color = 0xFF8E0509.toInt(),
        isSystem = true,
    )
    val BPA: Bank = Bank(
        id = 0L,
        name = "BPA",
        color = 0xFFBCD1DA.toInt(),
        isSystem = true,
    )
    val METROPOLITANO: Bank = Bank(
        id = 0L,
        name = "Metropolitano",
        color = 0xFF91D506.toInt(),
        isSystem = true,
    )

    /** All system banks in display order. */
    val ALL: List<Bank> = listOf(BANDEC, BPA, METROPOLITANO)
}
