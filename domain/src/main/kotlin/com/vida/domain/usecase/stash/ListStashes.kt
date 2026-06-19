package com.vida.domain.usecase.stash

import com.vida.domain.model.Stash
import com.vida.domain.repository.StashRepository
import kotlinx.coroutines.flow.Flow

/** Reactive stream of all stashes in the system. */
class ListStashes(private val repo: StashRepository) {
    operator fun invoke(): Flow<List<Stash>> = repo.getAll()
}