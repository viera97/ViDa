package com.vida.data.repository

import com.vida.data.db.dao.CardDao
import com.vida.data.mapper.CardMapper
import com.vida.domain.model.Card
import com.vida.domain.model.Money
import com.vida.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val dao: CardDao,
    private val mapper: CardMapper,
) : CardRepository {

    override fun getAll(): Flow<List<Card>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Card? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(card: Card): Long =
        dao.upsert(mapper.toEntity(card))

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun getBalance(id: Long, asOf: Instant): Money {
        // PR #1-2: in-memory computation deferred. PR #3: BalanceDao.
        TODO("BalanceDao integration in PR #3")
    }
}
