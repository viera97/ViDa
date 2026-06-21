package com.vida.data.repository

import com.vida.data.db.dao.BalanceDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.WalletMapper
import com.vida.data.mapper.util.fromMinorUnits
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Wallet
import com.vida.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject

class WalletRepositoryImpl @Inject constructor(
    private val dao: WalletDao,
    private val balanceDao: BalanceDao,
    private val mapper: WalletMapper,
) : WalletRepository {

    override suspend fun get(): Wallet =
        dao.get()?.let(mapper::toDomain)
            ?: throw NoSuchElementException("Wallet not found — call upsert first")

    override suspend fun upsert(wallet: Wallet) {
        require(wallet.id == 1L) { "Wallet must have id=1" }
        dao.upsert(mapper.toEntity(wallet))
    }

    override suspend fun getBalance(asOf: Instant): Money {
        val cupMinor = balanceDao.getWalletBalance(asOf.toEpochMilli())
            .first()?.totalCupMinor ?: 0L
        return Money.fromMinorUnits(cupMinor, Currency.CUP)
    }
}
