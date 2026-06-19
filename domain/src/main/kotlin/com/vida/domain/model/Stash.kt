package com.vida.domain.model

import java.time.Instant

/**
 * A stash — a named pool of money in a single currency. Used for savings goals,
 * petty cash, or any user-defined bucket. Balance is computed (Q7).
 */
data class Stash(
    val id: Long = 0L,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val currency: Currency,
) {
    init {
        require(name.isNotBlank()) { "Stash name must not be blank" }
        require(name.length <= 100) { "Stash name must be ≤ 100 chars" }
        require(!updatedAt.isBefore(createdAt)) { "Stash updatedAt must be ≥ createdAt" }
    }
}