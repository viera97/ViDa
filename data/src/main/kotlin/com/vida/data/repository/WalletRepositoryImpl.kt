package com.vida.data.repository

import com.vida.data.db.dao.BalanceDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.WalletMapper
import com.vida.data.mapper.util.fromMinorUnits
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import com.vida.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val dao: WalletDao,
    private val balanceDao: BalanceDao,
    private val mapper: WalletMapper,
) : WalletRepository {

    override fun getAll(): Flow<List<Wallet>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Wallet? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(wallet: Wallet) {
        dao.upsert(mapper.toEntity(wallet))
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    /**
     * Reactive observation of a wallet's balance. The currency is sourced from the
     * wallet row (looked up once per subscription); the minor-unit total comes from
     * [BalanceDao.getWalletBalance], which Room re-invalidates whenever the underlying
     * `wallets`, `transfers`, `expenses`, or `currency_rates` tables change.
     *
     * Emits [Money.ZERO_CUP] if the underlying flow throws, so consumers can keep
     * collecting without breaking the reactive chain (mirrors the pattern in
     * [com.vida.feature.walletmanagement.WalletViewModel]).
     */
    override fun observeBalance(id: Long): Flow<Money> = flow {
        val currencyCode = dao.getById(id)?.currency ?: "CUP"
        val currency = Currency.fromCode(currencyCode)
        emitAll(
            balanceDao.getWalletBalance(id)
                .map { entity ->
                    if (entity != null) {
                        Money.fromMinorUnits(entity.totalCupMinor, currency)
                    } else {
                        Money(BigDecimal.ZERO, currency)
                    }
                },
        )
    }.catch { emit(Money.ZERO_CUP) }
}
