package com.vida.domain.model

/**
 * The singleton Wallet — the user's primary cash pool in a single currency.
 *
 * v1 commits to a singleton (Q1 locked). Only one [Wallet] row exists at any time.
 * The default [id] of `1L` is the reserved singleton identifier; the [init] block
 * rejects any other value so the invariant is enforced at construction.
 */
data class Wallet(
    val id: Long = 1L,
    val currency: Currency,
    val name: String = "Billetera",
) {
    init {
        require(id == 1L) { "Wallet is a singleton; id must be 1" }
    }
}