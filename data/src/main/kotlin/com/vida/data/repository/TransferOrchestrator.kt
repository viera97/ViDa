package com.vida.data.repository

import androidx.room.withTransaction
import com.vida.data.db.AppDatabase
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.TransferDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.TransferMapper
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
 * Per Q7 (locked), balances are computed (not stored). In PR #3 v1 the
 * transaction body verifies that the source and destination exist, then inserts
 * the transfer row. The `@Transaction` is forward-compat: when stored-balance
 * columns are added in a future version, the debit/credit steps will join this
 * transaction (see vida-domain design #93 §7).
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
     * Records [transfer] atomically: verifies source + destination exist, then
     * inserts the transfer row inside a database transaction.
     *
     * @return the row id assigned by the database
     * @throws IllegalStateException if the source or destination does not exist
     */
    suspend fun recordTransfer(transfer: Transfer): Long =
        database.withTransaction {
            verifySource(transfer.fromType, transfer.fromId)
            verifyDestination(transfer.toType, transfer.toId)
            transferDao.upsert(transferMapper.toEntity(transfer))
        }

    private suspend fun verifySource(type: SourceType, id: Long?) {
        when (type) {
            SourceType.WALLET ->
                checkNotNull(walletDao.get()) { "Wallet not found — call upsert first" }
            SourceType.CARD ->
                checkNotNull(cardDao.getById(id!!)) { "Card $id not found" }
            SourceType.STASH ->
                checkNotNull(stashDao.getById(id!!)) { "Stash $id not found" }
        }
    }

    private suspend fun verifyDestination(type: SourceType, id: Long?) {
        when (type) {
            SourceType.WALLET ->
                checkNotNull(walletDao.get()) { "Wallet not found — call upsert first" }
            SourceType.CARD ->
                checkNotNull(cardDao.getById(id!!)) { "Card $id not found" }
            SourceType.STASH ->
                checkNotNull(stashDao.getById(id!!)) { "Stash $id not found" }
        }
    }
}
