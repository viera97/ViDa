package com.vida.domain.usecase.stash

import com.vida.domain.model.Money
import com.vida.domain.repository.StashRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the balance of the stash identified by [id].
 *
 * The returned [Flow] is reactive — Room invalidates the underlying query
 * whenever the `stashes`, `transfers`, `expenses`, or `currency_rates`
 * tables change, so consumers (typically ViewModels) automatically re-render
 * after a transfer, expense, or mutation made elsewhere in the app.
 *
 * Real implementation lives in `:data` as a Room SUM query (expenses +
 * transfers affecting this stash), wrapped behind
 * [StashRepository.observeBalance].
 */
class GetStashBalance(private val repo: StashRepository) {
    operator fun invoke(id: Long): Flow<Money> {
        require(id > 0) { "Stash id must be > 0" }
        return repo.observeBalance(id)
    }
}