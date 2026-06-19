package com.vida.domain.usecase.stash

import com.vida.domain.model.Currency
import com.vida.domain.model.Stash
import com.vida.domain.repository.StashRepository
import java.time.Instant

/** Creates a new stash with [name] and [currency]. */
class AddStash(private val repo: StashRepository) {
    suspend operator fun invoke(
        name: String,
        currency: Currency,
        now: Instant = Instant.now(),
    ): Long {
        require(name.isNotBlank()) { "Stash name must not be blank" }
        val stash = Stash(
            name = name,
            createdAt = now,
            updatedAt = now,
            currency = currency,
        )
        return repo.upsert(stash)
    }
}