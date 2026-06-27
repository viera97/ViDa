package com.vida.data.repository

import androidx.room.withTransaction
import com.vida.data.db.AppDatabase
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.TransferDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.TransferMapper
import com.vida.data.mapper.util.amountMinorUnits
import com.vida.domain.model.SourceType
import com.vida.domain.model.Transfer
import javax.inject.Inject

/**
 * Data-layer dispatcher that records a [Transfer] atomically.
 *
 * This is NOT a domain use case — it is a `:data`-layer coordinator. It receives
 * `TransferDao` + `WalletDao` + `CardDao` + `StashDao` + [AppDatabase] via
 * constructor injection and wraps the recording in [database.withTransaction].
 *
 * Per Option C in the balance-tracking decision, wallet and card balances are
 * stored directly in `balance_minor` and auto-updated by transfers (ledger
 * semantics): the source's `balance_minor` decreases by the transfer amount,
 * the destination's `balance_minor` increases by the same amount. Stashes do
 * NOT have a `balance_minor` column — their balance is still computed from
 * transfers in `BalanceDao` SQL — so the orchestrator skips the debit/credit
 * step for stash sides.
 *
 * If any step fails, the entire transaction rolls back — no partial state leaks.
 */
class TransferOrchestrator @Inject constructor(
    private val database: AppDatabase,
    private val transferDao: TransferDao,
    private val walletDao: WalletDao,
    private val cardDao: CardDao,
    private val stashDao: StashDao,
    private val transferMapper: TransferMapper,
) {

    /**
     * Records [transfer] atomically: verifies source + destination exist, inserts
     * the transfer row, and applies the ledger delta to the source and destination
     * balances — all inside a single [database.withTransaction].
     *
     * @return the row id assigned by the database
     * @throws IllegalStateException if the source or destination does not exist
     */
    suspend fun recordTransfer(transfer: Transfer): Long =
        database.withTransaction {
            verifySource(transfer.fromType, transfer.fromId)
            verifyDestination(transfer.toType, transfer.toId)
            val newId = transferDao.upsert(transferMapper.toEntity(transfer))
            // Apply ledger delta (Option C). Stash sides are skipped — they have no
            // stored balance column. The minus sign reduces the source; the positive
            // value increases the destination.
            applyDelta(transfer.fromType, transfer.fromId, -transfer.amount.amountMinorUnits())
            applyDelta(transfer.toType, transfer.toId, transfer.amount.amountMinorUnits())
            newId
        }

    /**
     * Applies [delta] (in minor units) to the stored `balance_minor` of the source
     * row identified by ([type], [id]). No-op for `STASH` — stash balance is
     * computed from the transfers table at read time (see `BalanceDao`).
     */
    private suspend fun applyDelta(type: SourceType, id: Long, delta: Long) {
        when (type) {
            SourceType.WALLET -> walletDao.adjustBalance(id, delta)
            SourceType.CARD -> cardDao.adjustBalance(id, delta)
            SourceType.STASH -> Unit
        }
    }

    /**
     * Confirms the source side of [Transfer] refers to an existing entity.
     *
     * All source kinds — wallet, card, stash — are looked up by id. The wallet
     * is just another source row in the `wallets` table; it has its own id and
     * is verified the same way as a card or stash.
     */
    private suspend fun verifySource(type: SourceType, id: Long) {
        when (type) {
            SourceType.WALLET ->
                checkNotNull(walletDao.getById(id)) { "Wallet $id not found" }
            SourceType.CARD ->
                checkNotNull(cardDao.getById(id)) { "Card $id not found" }
            SourceType.STASH ->
                checkNotNull(stashDao.getById(id)) { "Stash $id not found" }
        }
    }

    /**
     * Confirms the destination side of [Transfer] refers to an existing entity.
     *
     * All destination kinds — wallet, card, stash — are looked up by id. The
     * wallet is just another source row in the `wallets` table; it has its own
     * id and is verified the same way as a card or stash.
     */
    private suspend fun verifyDestination(type: SourceType, id: Long) {
        when (type) {
            SourceType.WALLET ->
                checkNotNull(walletDao.getById(id)) { "Wallet $id not found" }
            SourceType.CARD ->
                checkNotNull(cardDao.getById(id)) { "Card $id not found" }
            SourceType.STASH ->
                checkNotNull(stashDao.getById(id)) { "Stash $id not found" }
        }
    }
}