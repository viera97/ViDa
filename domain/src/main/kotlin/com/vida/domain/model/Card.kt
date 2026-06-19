package com.vida.domain.model

import java.time.LocalDate

/**
 * A payment card (debit, credit, or prepaid). Balance is intentionally NOT a field:
 * it is computed by `:data` via a Room SUM query aggregating expenses + transfers
 * that affect this card (Q7 locked).
 */
data class Card(
    val id: Long = 0L,
    val number: CardNumber,
    val bank: String,
    val type: CardType,
    val currency: Currency,
    val note: String? = null,
    val expirationDate: LocalDate,
) {
    init {
        require(bank.isNotBlank()) { "Card bank must not be blank" }
        require(note == null || note.length <= 200) { "Card note must be ≤ 200 chars" }
    }
}