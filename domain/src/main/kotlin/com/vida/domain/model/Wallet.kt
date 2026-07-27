package com.vida.domain.model

/**
 * A user-managed cash pool in a single currency.
 *
 * v2 supports multiple wallets via AUTOINCREMENT PK. Each wallet stores its own
 * [balance] in [currency] — the user maintains it manually via the edit dialog.
 * Transfers and expenses still record normally but do NOT auto-update [balance]
 * (Option B in the balance-tracking decision); see
 * [com.vida.data.db.dao.BalanceDao] for the read-side SQL.
 */
data class Wallet(
    val id: Long = 0L,
    val currency: String = "CUP",
    val name: String = "Billetera",
    val balance: Money = Money.ZERO_CUP,
)