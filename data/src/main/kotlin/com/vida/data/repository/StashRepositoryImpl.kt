package com.vida.data.repository

import com.vida.data.db.dao.BalanceDao
import com.vida.data.db.dao.StashDao
import com.vida.data.mapper.StashMapper
import com.vida.data.mapper.util.fromMinorUnits
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import com.vida.domain.repository.StashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

class StashRepositoryImpl @Inject constructor(
    private val dao: StashDao,
    private val balanceDao: BalanceDao,
    private val mapper: StashMapper,
) : StashRepository {

    override fun getAll(): Flow<List<Stash>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Stash? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(stash: Stash): Long =
        dao.upsert(mapper.toEntity(stash))

    override suspend fun delete(id: Long) = dao.delete(id)

    /**
     * Reactive observation of a stash's balance. The BalanceDao SQL already
     * converts the per-stash total to CUP, so we always reconstruct as
     * [Currency.CUP]. The underlying [BalanceDao.getStashBalance] Flow is
     * invalidated by Room whenever the `stashes`, `transfers`, `expenses`,
     * or `currency_rates` tables change.
     *
     * Emits [Money.ZERO_CUP] if the underlying flow throws, so consumers can
     * keep collecting without breaking the reactive chain.
     */
    override fun observeBalance(id: Long): Flow<Money> = flow {
        emitAll(
            balanceDao.getStashBalance(id, Instant.now().toEpochMilli())
                .map { entity ->
                    if (entity != null) {
                        Money.fromMinorUnits(entity.totalCupMinor, Currency.CUP)
                    } else {
                        Money(BigDecimal.ZERO, Currency.CUP)
                    }
                },
        )
    }.catch { emit(Money.ZERO_CUP) }
}
