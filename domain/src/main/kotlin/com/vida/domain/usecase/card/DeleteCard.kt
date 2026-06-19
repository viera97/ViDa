package com.vida.domain.usecase.card

import com.vida.domain.repository.CardRepository

/** Removes a card by its row id. */
class DeleteCard(private val repo: CardRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "Card id must be > 0" }
        repo.delete(id)
    }
}