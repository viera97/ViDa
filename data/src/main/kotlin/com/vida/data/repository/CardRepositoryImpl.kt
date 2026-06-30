package com.vida.data.repository

import com.vida.data.db.dao.BalanceDao
import com.vida.data.db.dao.CardDao
import com.vida.data.mapper.CardMapper
import com.vida.data.mapper.util.fromMinorUnits
import com.vida.domain.model.Card
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val dao: CardDao,
    private val balanceDao: BalanceDao,
    private val mapper: CardMapper,
) : CardRepository {

    override fun getAll(): Flow<List<Card>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Card? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(card: Card): Long =
        dao.upsert(mapper.toEntity(card))

    override suspend fun delete(id: Long) = dao.delete(id)

    /**
     * Reactive observation of a card's balance. The currency is sourced from the
     * card row (looked up once per subscription); the minor-unit total comes from
     * [BalanceDao.getCardBalance], which Room re-invalidates whenever the underlying
     * `cards`, `transfers`, `expenses`, or `currency_rates` tables change.
     *
     * Emits [Money.ZERO_CUP] if the underlying flow throws, so consumers can keep
     * collecting without breaking the reactive chain (mirrors the pattern in
     * [com.vida.feature.cardmanagement.CardListViewModel]).
     */
    override fun observeBalance(id: Long): Flow<Money> = flow {
        val currency = dao.getById(id)?.currency ?: Currency.CUP
        emitAll(
            balanceDao.getCardBalance(id)
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
