package com.vida.domain.usecase.card

import com.vida.domain.model.Money
import com.vida.domain.repository.CardRepository
import java.time.Instant

/**
 * Computes the balance for a card. Real implementation lives in :data as a
 * Room SUM query (expenses + transfers affecting this card). This use case
 * delegates to the repo; the repo throws NotImplementedError in PR #1 because
 * no Room impl exists yet.
 */
class GetCardBalance(private val repo: CardRepository) {
    suspend operator fun invoke(id: Long, asOf: Instant = Instant.now()): Money {
        require(id > 0) { "Card id must be > 0" }
        return repo.getBalance(id, asOf)
    }
}