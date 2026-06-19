package com.vida.domain.usecase.card

import com.vida.domain.model.Card
import com.vida.domain.repository.CardRepository
import java.time.LocalDate

/**
 * Registers a new card in the system. Rejects cards whose expiration date is
 * more than a year in the past (i.e., obviously already expired).
 *
 * @return the row id assigned by the persistence layer.
 */
class AddCard(private val repo: CardRepository) {
    suspend operator fun invoke(card: Card): Long {
        require(card.expirationDate.isAfter(LocalDate.now().minusYears(1))) {
            "Card expiration date cannot be more than 1 year in the past"
        }
        return repo.upsert(card)
    }
}