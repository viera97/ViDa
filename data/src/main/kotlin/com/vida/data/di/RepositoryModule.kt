package com.vida.data.di

import com.vida.data.repository.CardRepositoryImpl
import com.vida.data.repository.CategoryRepositoryImpl
import com.vida.data.repository.CurrencyRateRepositoryImpl
import com.vida.data.repository.ExpenseRepositoryImpl
import com.vida.data.repository.RefundRepositoryImpl
import com.vida.data.repository.StashRepositoryImpl
import com.vida.data.repository.WalletRepositoryImpl
import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.CurrencyRateRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.RefundRepository
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
    @Binds abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds abstract fun bindRefundRepository(impl: RefundRepositoryImpl): RefundRepository
    @Binds abstract fun bindCurrencyRateRepository(impl: CurrencyRateRepositoryImpl): CurrencyRateRepository
}
