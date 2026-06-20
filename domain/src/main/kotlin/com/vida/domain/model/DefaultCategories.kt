package com.vida.domain.model

/**
 * Seed set of system categories inserted on first run via
 * `SeedDefaultCategories`. The id is the placeholder `0L`; the persistence layer
 * assigns the real row id on insert.
 *
 * Names are kept in Spanish to match the app UI (Q-locale decision from PR #1).
 */
object DefaultCategories {
    val FOOD: Category = Category(
        id = 0L,
        name = "Comida",
        color = 0xFFE57373.toInt(),
        icon = "restaurant",
        isSystem = true,
    )
    val TRANSPORT: Category = Category(
        id = 0L,
        name = "Transporte",
        color = 0xFF64B5F6.toInt(),
        icon = "directions_bus",
        isSystem = true,
    )
    val HOUSING: Category = Category(
        id = 0L,
        name = "Vivienda",
        color = 0xFF81C784.toInt(),
        icon = "home",
        isSystem = true,
    )
    val HEALTH: Category = Category(
        id = 0L,
        name = "Salud",
        color = 0xFFF06292.toInt(),
        icon = "local_hospital",
        isSystem = true,
    )
    val ENTERTAINMENT: Category = Category(
        id = 0L,
        name = "Ocio",
        color = 0xFFBA68C8.toInt(),
        icon = "movie",
        isSystem = true,
    )
    val OTHER: Category = Category(
        id = 0L,
        name = "Otros",
        color = 0xFF90A4AE.toInt(),
        icon = "category",
        isSystem = true,
    )

    /** All system categories in display order. */
    val ALL: List<Category> = listOf(FOOD, TRANSPORT, HOUSING, HEALTH, ENTERTAINMENT, OTHER)
}