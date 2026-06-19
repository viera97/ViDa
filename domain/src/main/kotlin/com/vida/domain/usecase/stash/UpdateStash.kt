package com.vida.domain.usecase.stash

import com.vida.domain.model.Stash
import com.vida.domain.repository.StashRepository
import java.time.Instant

/** Updates an existing stash; refreshes [Stash.updatedAt] to [now]. */
class UpdateStash(private val repo: StashRepository) {
    suspend operator fun invoke(stash: Stash, now: Instant = Instant.now()): Long {
        require(stash.id > 0) { "Stash id must be > 0" }
        require(stash.name.isNotBlank()) { "Stash name must not be blank" }
        return repo.upsert(stash.copy(updatedAt = now))
    }
}