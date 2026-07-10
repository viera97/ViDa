package com.vida.domain.repository

import com.vida.domain.model.Bank
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [Bank] aggregates. Implemented in `:data` (Room).
 *
 * Reactive [getAll] emits on every underlying table change; consumers
 * (`ListBanks`) collect it as state.
 */
interface BankRepository {
    fun getAll(): Flow<List<Bank>>
    suspend fun getById(id: Long): Bank?
    suspend fun getByName(name: String): Bank?
    suspend fun upsert(bank: Bank): Long
    suspend fun delete(id: Long)
}
