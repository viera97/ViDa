package com.vida.data.db.entity

import androidx.room.ColumnInfo

/**
 * Result row for [com.vida.data.db.dao.BalanceDao]'s aggregate total-balance query.
 *
 * `total_cup_minor` is the sum of all source balances (wallets + cards + stashes)
 * converted to CUP using the latest rate for each source's currency before `asOf`.
 * A source whose currency has no rate in `currency_rates` contributes 0, except
 * CUP sources which use an implicit rate of 1.0.
 */
data class CupTotalEntity(
    @ColumnInfo(name = "total_cup_minor") val totalCupMinor: Long,
)
