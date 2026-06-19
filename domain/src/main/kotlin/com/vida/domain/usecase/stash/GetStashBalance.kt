package com.vida.domain.usecase.stash

import com.vida.domain.model.Money
import com.vida.domain.repository.StashRepository
import java.time.Instant

/**
 * Computes the balance for a stash. Real implementation lives in :data as a
 * Room SUM query (expenses + transfers affecting this stash). The repo throws
 * NotImplementedError in PR #1 because no Room impl exists yet.
 */
class GetStashBalance(private val repo: StashRepository) {
    suspend operator fun invoke(id: Long, asOf: Instant = Instant.now()): Money {
        require(id > 0) { "Stash id must be > 0" }
        return repo.getBalance(id, asOf)
    }
}