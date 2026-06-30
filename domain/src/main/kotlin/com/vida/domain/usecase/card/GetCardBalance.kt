package com.vida.domain.usecase.card

import com.vida.domain.model.Money
import com.vida.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the balance of the card identified by [id].
 *
 * The returned [Flow] is reactive — Room invalidates the underlying query
 * whenever the `cards`, `transfers`, `expenses`, or `currency_rates` tables
 * change, so consumers (typically ViewModels) automatically re-render after
 * a transfer, expense, or mutation made elsewhere in the app.
 *
 * Real implementation lives in `:data` as a Room SUM query (expenses +
 * transfers affecting this card), wrapped behind
 * [CardRepository.observeBalance].
 */
class GetCardBalance(private val repo: CardRepository) {
    operator fun invoke(id: Long): Flow<Money> {
        require(id > 0) { "Card id must be > 0" }
        return repo.observeBalance(id)
    }
}