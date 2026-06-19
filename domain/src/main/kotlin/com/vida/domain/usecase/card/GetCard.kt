package com.vida.domain.usecase.card

import com.vida.domain.model.Card
import com.vida.domain.repository.CardRepository

/** Loads a single card by id; returns null if not found. */
class GetCard(private val repo: CardRepository) {
    suspend operator fun invoke(id: Long): Card? {
        require(id > 0) { "Card id must be > 0" }
        return repo.getById(id)
    }
}