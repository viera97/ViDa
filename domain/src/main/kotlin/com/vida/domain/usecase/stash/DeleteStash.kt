package com.vida.domain.usecase.stash

import com.vida.domain.repository.StashRepository

/** Removes a stash by its row id. */
class DeleteStash(private val repo: StashRepository) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "Stash id must be > 0" }
        repo.delete(id)
    }
}