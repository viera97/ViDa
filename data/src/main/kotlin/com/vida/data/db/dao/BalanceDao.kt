package com.vida.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.vida.data.db.entity.CupTotalEntity
import kotlinx.coroutines.flow.Flow

/**
 * Balance queries across sources (wallets + cards + stashes).
 *
 * Schema v7 changed the semantics: wallet and card balances are now stored directly
 * in the `balance_minor` column and no longer derived from transfer/expense math.
 * Schema v8 added the `incomes` table; incomes credit the destination's stored
 * balance for wallet/card (see [com.vida.data.repository.IncomeRepositoryImpl])
 * and add a positive term to the stash balance SQL formula at read time. Stash
 * balances are still computed from transfers, expenses, and now incomes (stashes
 * have no `balance_minor` column — out of scope for stored balances).
 *
 * Per Option C in the balance-tracking decision, wallet and card `balance_minor`
 * is auto-updated by `TransferOrchestrator` (debit source / credit destination),
 * by `ExpenseRepositoryImpl` (debit source), and by `IncomeRepositoryImpl`
 * (credit destination). Stash balance is still derived at read time from the
 * transfers, expenses, and incomes tables.
 *
 * For [observeTotalBalanceInCup], the stash subquery still applies currency
 * conversion to CUP via `currency_rates`; wallet and card subqueries return their
 * stored value verbatim. Note that the result is the **mixed-currency sum** of
 * (wallet stored balances) + (card stored balances) + (stash computed balances in
 * CUP). The result column is still named `total_cup_minor` for backwards
 * compatibility with the existing [CupTotalEntity] shape, but its semantics are no
 * longer strictly "everything converted to CUP" — wallet/card contributions are in
 * their native currency.
 *
 * Refunds are not subtracted anywhere (deferred — W1).
 */
@Dao
interface BalanceDao {

    /**
     * Returns the aggregate total across wallets, cards, and stashes as of [asOf]
     * (epoch millis). Stash contribution is still computed (transfers − expenses)
     * and converted to CUP via the latest currency rate. Wallet and card
     * contributions are read directly from their stored `balance_minor`. The
     * result column is named `total_cup_minor` but is the mixed-currency sum of
     * wallet/card stored values (in their native currencies) plus stash balances
     * converted to CUP.
     *
     * Emits `CupTotalEntity(0)` when all sources are empty or no rates are found.
     */
    @Query(
        """
        SELECT CAST(COALESCE(
            (SELECT COALESCE(SUM(w_bal.balance_minor), 0)
             FROM (
                SELECT w.balance_minor AS balance_minor
                FROM wallets w
             ) w_bal
            )
            + (
            SELECT COALESCE(SUM(c_bal.balance_minor), 0)
             FROM (
                SELECT c.balance_minor AS balance_minor
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
                        + (SELECT COALESCE(SUM(i.amount_minor), 0) FROM incomes i
                           WHERE i.destination_stash_id = s.id AND i.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM stashes s
             ) s_bal
            )
        , 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun observeTotalBalanceInCup(asOf: Long): Flow<CupTotalEntity?>

    /**
     * Returns the stored balance of a single card [cardId] as of [asOf]
     * (epoch millis). The value is read directly from `cards.balance_minor` — no
     * currency conversion and no transfer/expense math is applied. The balance
     * is auto-updated by `TransferOrchestrator` (debit on transfer out, credit on
     * transfer in), by `ExpenseRepositoryImpl` (debit on expense), and by
     * `IncomeRepositoryImpl` (credit on income, schema v8+); the user can also
     * set the balance manually via the card edit dialog. [asOf] is currently
     * unused by the query but kept in the signature for API compatibility. Emits
     * `CupTotalEntity(0)` when no card row matches [cardId].
     */
    @Query(
        """
        SELECT CAST(COALESCE(
            (SELECT c.balance_minor FROM cards c WHERE c.id = :cardId)
        , 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun getCardBalance(cardId: Long): Flow<CupTotalEntity?>

    /**
     * Returns the balance of a single stash [stashId] converted to CUP as of [asOf]
     * (epoch millis). Same formula as [getCardBalance] but for the `stashes` table,
     * with the income-positive term added (schema v8).
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
                        + (SELECT COALESCE(SUM(i.amount_minor), 0) FROM incomes i
                           WHERE i.destination_stash_id = s.id AND i.date_time <= :asOf)
                    , 0) AS balance_minor
                FROM stashes s
                WHERE s.id = :stashId
             ) s_bal
        ), 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun getStashBalance(stashId: Long, asOf: Long): Flow<CupTotalEntity?>

    /**
     * Returns the stored balance of a single wallet [walletId] as of [asOf]
     * (epoch millis). The value is read directly from `wallets.balance_minor` — no
     * currency conversion and no transfer/expense math is applied. The balance
     * is auto-updated by `TransferOrchestrator` (debit on transfer out, credit on
     * transfer in), by `ExpenseRepositoryImpl` (debit on expense), and by
     * `IncomeRepositoryImpl` (credit on income, schema v8+); the user can also
     * set the balance manually via the wallet edit dialog. [asOf] is currently
     * unused by the query but kept in the signature for API compatibility. Emits
     * `CupTotalEntity(0)` when no wallet row matches [walletId].
     */
    @Query(
        """
        SELECT CAST(COALESCE(
            (SELECT w.balance_minor FROM wallets w WHERE w.id = :walletId)
        , 0) AS INTEGER) AS total_cup_minor
        """,
    )
    fun getWalletBalance(walletId: Long): Flow<CupTotalEntity?>
}
