package com.vida.domain.usecase.card

import com.vida.domain.model.Card
import com.vida.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow

/** Reactive stream of all cards in the system. */
class ListCards(private val repo: CardRepository) {
    operator fun invoke(): Flow<List<Card>> = repo.getAll()
}