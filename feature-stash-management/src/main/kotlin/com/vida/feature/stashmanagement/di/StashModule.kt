package com.vida.feature.stashmanagement.di

import com.vida.domain.repository.StashRepository
import com.vida.domain.usecase.stash.AddStash
import com.vida.domain.usecase.stash.DeleteStash
import com.vida.domain.usecase.stash.GetStash
import com.vida.domain.usecase.stash.UpdateStash
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing stash-specific use cases for ViewModels.
 *
 * NOTE: [com.vida.domain.usecase.stash.ListStashes] is NOT provided here —
 * it is already provided by [com.vida.feature.home.di.HomeModule] in the
 * same [ViewModelComponent]. Duplicating it would cause a Dagger binding error.
 */
@Module
@InstallIn(ViewModelComponent::class)
object StashModule {

    @Provides
    fun provideAddStash(repo: StashRepository): AddStash =
        AddStash(repo)

    @Provides
    fun provideUpdateStash(repo: StashRepository): UpdateStash =
        UpdateStash(repo)

    @Provides
    fun provideDeleteStash(repo: StashRepository): DeleteStash =
        DeleteStash(repo)

    @Provides
    fun provideGetStash(repo: StashRepository): GetStash =
        GetStash(repo)
}
