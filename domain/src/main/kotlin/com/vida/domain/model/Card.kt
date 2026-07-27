package com.vida.domain.model

import java.time.LocalDate

/**
 * A payment card (debit, credit, or prepaid). Stores its own [balance] in [currency]
 * — the user maintains it manually via the edit dialog. Transfers and expenses still
 * record normally but do NOT auto-update [balance] (Option B in the balance-tracking
 * decision); see [com.vida.data.db.dao.BalanceDao] for the read-side SQL.
 */
data class Card(
    val id: Long = 0L,
    val number: CardNumber,
    val bank: String,
    val type: CardType,
    val currency: String,
    val note: String? = null,
    val expirationDate: LocalDate,
    val balance: Money = Money.ZERO_CUP,
) {
    init {
        require(bank.isNotBlank()) { "Card bank must not be blank" }
        require(note == null || note.length <= 200) { "Card note must be ≤ 200 chars" }
    }
}