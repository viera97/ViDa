package com.vida.data.di

import com.vida.data.repository.CardRepositoryImpl
import com.vida.data.repository.StashRepositoryImpl
import com.vida.data.repository.WalletRepositoryImpl
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository
    @Binds abstract fun bindStashRepository(impl: StashRepositoryImpl): StashRepository
    @Binds abstract fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository
}
