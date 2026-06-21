package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.vida.data.db.entity.CupTotalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Aggregate balance query across all sources (wallets + cards + stashes).
 *
 * Per design decision T5, the total balance is computed in SQL using a correlated
 * `getLatestRate` subquery. Each source's balance is derived from its expenses and
 * transfer participations (Q7 — balance is computed, not stored):
 *
 * - source balance = (transfers IN) - (transfers OUT) - (expenses FROM this source)
 *
 * Each source's balance is then converted to CUP using the latest rate for that
 * source's currency to CUP before [asOf]. CUP sources use an implicit rate of 1.0
 * when no explicit CUP-to-CUP rate row exists. Sources whose currency has no rate
 * contribute 0 (per SCN-DATA-PR3-009).
 *
 * **Note**: This is a computed-balance implementation (the wallet/card/stash
 * entities have no `amount_minor` column per Q7 locked). The spec (#106) SQL
 * referenced `w.amount_minor` which does not exist in the actual schema shipped
 * in PR #1. This query computes balances from the real source-of-truth tables
 * (`expenses` + `transfers`). Refunds are deferred to a future enhancement.
 */
@Dao
interface BalanceDao {

    /**
     * Returns the total balance across all sources converted to CUP as of [asOf]
     * (epoch millis). Emits `CupTotalEntity(0)` when all sources are empty or no
     * rates are found.
     */
    @Query(
        """
        SELECT CAST(COALESCE(
            (SELECT COALESCE(SUM(
                w_bal.balance_minor * COALESCE(
                    (SELECT cr.rate FROM currency_rates cr
                     WHERE cr.from_currency = w_bal.currency
                       AND cr.to_currency = 'CUP'
                       AND cr.effective_date <= :asOf
                     ORDER BY cr.effective_date DESC LIMIT 1),
                    CASE WHEN w_bal.currency = 'CUP' THEN 1.0 ELSE 0.0 END
                )
            ), 0)
             FROM (
                SELECT w.id AS id, w.currency AS currency,
                    COALESCE(
                        (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                         WHERE t.destination_wallet_id = w.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                           WHERE t.source_wallet_id = w.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(e.amount_minor), 0) FROM expenses e
                           WHERE e.source_wallet_id = w.id AND e.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM wallets w
             ) w_bal
            )
            + (
            SELECT COALESCE(SUM(
                c_bal.balance_minor * COALESCE(
                    (SELECT cr.rate FROM currency_rates cr
                     WHERE cr.from_currency = c_bal.currency
                       AND cr.to_currency = 'CUP'
                       AND cr.effective_date <= :asOf
                     ORDER BY cr.effective_date DESC LIMIT 1),
                    CASE WHEN c_bal.currency = 'CUP' THEN 1.0 ELSE 0.0 END
                )
            ), 0)
             FROM (
                SELECT c.id AS id, c.currency AS currency,
                    COALESCE(
                        (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                         WHERE t.destination_card_id = c.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                           WHERE t.source_card_id = c.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(e.amount_minor), 0) FROM expenses e
                           WHERE e.source_card_id = c.id AND e.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM cards c
             ) c_bal
            )
            + (
            SELECT COALESCE(SUM(
                s_bal.balance_minor * COALESCE(
                    (SELECT cr.rate FROM currency_rates cr
                     WHERE cr.from_currency = s_bal.currency
                       AND cr.to_currency = 'CUP'
                       AND cr.effective_date <= :asOf
                     ORDER BY cr.effective_date DESC LIMIT 1),
                    CASE WHEN s_bal.currency = 'CUP' THEN 1.0 ELSE 0.0 END
                )
            ), 0)
             FROM (
                SELECT s.id AS id, s.currency AS currency,
                    COALESCE(
                        (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                         WHERE t.destination_stash_id = s.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                           WHERE t.source_stash_id = s.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(e.amount_minor), 0) FROM expenses e
                           WHERE e.source_stash_id = s.id AND e.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM stashes s
             ) s_bal
            )
        , 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun observeTotalBalanceInCup(asOf: Long): Flow<CupTotalEntity?>

    /**
     * Returns the balance of a single card [cardId] converted to CUP as of [asOf]
     * (epoch millis). Mirrors [observeTotalBalanceInCup]'s per-source formula:
     *
     *   card balance = (transfers IN) - (transfers OUT) - (expenses FROM this card)
     *
     * then multiplied by the latest `currency_rates` row for the card's currency →
     * CUP effective on or before [asOf]. CUP cards use an implicit rate of 1.0.
     * Emits `CupTotalEntity(0)` when the card has no activity or no rate is found
     * (per SCN-DATA-PR3-009). Refunds are not subtracted (deferred — W1).
     */
    @Query(
        """
        SELECT CAST(COALESCE(
            (SELECT COALESCE(SUM(
                c_bal.balance_minor * COALESCE(
                    (SELECT cr.rate FROM currency_rates cr
                     WHERE cr.from_currency = c_bal.currency
                       AND cr.to_currency = 'CUP'
                       AND cr.effective_date <= :asOf
                     ORDER BY cr.effective_date DESC LIMIT 1),
                    CASE WHEN c_bal.currency = 'CUP' THEN 1.0 ELSE 0.0 END
                )
            ), 0)
             FROM (
                SELECT c.id AS id, c.currency AS currency,
                    COALESCE(
                        (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                         WHERE t.destination_card_id = c.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                           WHERE t.source_card_id = c.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(e.amount_minor), 0) FROM expenses e
                           WHERE e.source_card_id = c.id AND e.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM cards c
                WHERE c.id = :cardId
             ) c_bal
        ), 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun getCardBalance(cardId: Long, asOf: Long): Flow<CupTotalEntity?>

    /**
     * Returns the balance of a single stash [stashId] converted to CUP as of [asOf]
     * (epoch millis). Same formula as [getCardBalance] but for the `stashes` table.
     * Refunds are not subtracted (deferred — W1).
     */
    @Query(
        """
        SELECT CAST(COALESCE(
            (SELECT COALESCE(SUM(
                s_bal.balance_minor * COALESCE(
                    (SELECT cr.rate FROM currency_rates cr
                     WHERE cr.from_currency = s_bal.currency
                       AND cr.to_currency = 'CUP'
                       AND cr.effective_date <= :asOf
                     ORDER BY cr.effective_date DESC LIMIT 1),
                    CASE WHEN s_bal.currency = 'CUP' THEN 1.0 ELSE 0.0 END
                )
            ), 0)
             FROM (
                SELECT s.id AS id, s.currency AS currency,
                    COALESCE(
                        (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                         WHERE t.destination_stash_id = s.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                           WHERE t.source_stash_id = s.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(e.amount_minor), 0) FROM expenses e
                           WHERE e.source_stash_id = s.id AND e.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM stashes s
                WHERE s.id = :stashId
             ) s_bal
        ), 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun getStashBalance(stashId: Long, asOf: Long): Flow<CupTotalEntity?>

    /**
     * Returns the balance of the singleton wallet (id = 1) converted to CUP as of
     * [asOf] (epoch millis). Same formula as [getCardBalance] but for the `wallets`
     * table, filtered to the singleton row. Emits `CupTotalEntity(0)` when no
     * wallet row exists yet. Refunds are not subtracted (deferred — W1).
     */
    @Query(
        """
        SELECT CAST(COALESCE(
            (SELECT COALESCE(SUM(
                w_bal.balance_minor * COALESCE(
                    (SELECT cr.rate FROM currency_rates cr
                     WHERE cr.from_currency = w_bal.currency
                       AND cr.to_currency = 'CUP'
                       AND cr.effective_date <= :asOf
                     ORDER BY cr.effective_date DESC LIMIT 1),
                    CASE WHEN w_bal.currency = 'CUP' THEN 1.0 ELSE 0.0 END
                )
            ), 0)
             FROM (
                SELECT w.id AS id, w.currency AS currency,
                    COALESCE(
                        (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                         WHERE t.destination_wallet_id = w.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(t.amount_minor), 0) FROM transfers t
                           WHERE t.source_wallet_id = w.id AND t.date_time <= :asOf)
                        - (SELECT COALESCE(SUM(e.amount_minor), 0) FROM expenses e
                           WHERE e.source_wallet_id = w.id AND e.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM wallets w
                WHERE w.id = 1
             ) w_bal
        ), 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun getWalletBalance(asOf: Long): Flow<CupTotalEntity?>
}
