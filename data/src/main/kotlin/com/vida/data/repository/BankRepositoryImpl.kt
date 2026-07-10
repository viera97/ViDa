package com.vida.data.repository

import com.vida.data.db.dao.BankDao
import com.vida.data.mapper.BankMapper
import com.vida.domain.model.Bank
import com.vida.domain.repository.BankRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BankRepositoryImpl @Inject constructor(
    private val dao: BankDao,
    private val mapper: BankMapper,
) : BankRepository {

    override fun getAll(): Flow<List<Bank>> =
        dao.observeAll().map { entities -> entities.map { mapper.toDomain(it) } }

    override suspend fun getById(id: Long): Bank? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun getByName(name: String): Bank? =
        dao.getByName(name)?.let(mapper::toDomain)

    override suspend fun upsert(bank: Bank): Long =
        dao.upsert(mapper.toEntity(bank))

    override suspend fun delete(id: Long) = dao.delete(id)
}
