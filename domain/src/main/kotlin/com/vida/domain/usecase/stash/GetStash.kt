package com.vida.domain.usecase.stash

import com.vida.domain.model.Stash
import com.vida.domain.repository.StashRepository

/** Loads a single stash by id; returns null if not found. */
class GetStash(private val repo: StashRepository) {
    suspend operator fun invoke(id: Long): Stash? {
        require(id > 0) { "Stash id must be > 0" }
        return repo.getById(id)
    }
}