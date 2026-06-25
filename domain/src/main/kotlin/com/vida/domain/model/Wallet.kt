package com.vida.domain.model

/**
 * A user-managed cash pool in a single currency.
 *
 * v2 supports multiple wallets via AUTOINCREMENT PK. Each wallet tracks its own
 * balance independently through expenses and transfers.
 */
data class Wallet(
    val id: Long = 0L,
    val currency: Currency,
    val name: String = "Billetera",
    val initialBalance: Money = Money.ZERO_CUP,
)