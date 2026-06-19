package com.vida.domain.usecase.card

import com.vida.domain.model.Card
import com.vida.domain.repository.CardRepository

/**
 * Updates an existing card. The [Card.id] must be a positive row id — use [AddCard]
 * for new cards (id == 0).
 */
class UpdateCard(private val repo: CardRepository) {
    suspend operator fun invoke(card: Card): Long {
        require(card.id > 0) { "Card id must be > 0 to update (use AddCard for new cards)" }
        return repo.upsert(card)
    }
}