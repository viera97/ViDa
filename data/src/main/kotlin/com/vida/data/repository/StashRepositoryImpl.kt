package com.vida.data.repository

import com.vida.data.db.dao.StashDao
import com.vida.data.mapper.StashMapper
import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import com.vida.domain.repository.StashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class StashRepositoryImpl @Inject constructor(
    private val dao: StashDao,
    private val mapper: StashMapper,
) : StashRepository {

    override fun getAll(): Flow<List<Stash>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Stash? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(stash: Stash): Long =
        dao.upsert(mapper.toEntity(stash))

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun getBalance(id: Long, asOf: Instant): Money {
        TODO("BalanceDao integration in PR #3")
    }
}
