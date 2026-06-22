package com.vida.feature.home.di

import com.vida.domain.repository.CardRepository
import com.vida.domain.repository.CurrencyRateRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.StashRepository
import com.vida.domain.repository.WalletRepository
import com.vida.domain.usecase.ConvertCurrency
import com.vida.domain.usecase.balance.GetTotalBalance
import com.vida.domain.usecase.card.GetCardBalance
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.expense.ListExpenses
import com.vida.domain.usecase.rate.GetCurrentRate
import com.vida.domain.usecase.stash.GetStashBalance
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWalletBalance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object HomeModule {

    @Provides
    fun provideConvertCurrency(repo: CurrencyRateRepository): ConvertCurrency =
        ConvertCurrency(repo)

    @Provides
    fun provideGetTotalBalance(
        cardRepo: CardRepository,
        stashRepo: StashRepository,
        walletRepo: WalletRepository,
        convertCurrency: ConvertCurrency,
    ): GetTotalBalance = GetTotalBalance(cardRepo, stashRepo, walletRepo, convertCurrency)

    @Provides
    fun provideListExpenses(repo: ExpenseRepository): ListExpenses =
        ListExpenses(repo)

    @Provides
    fun provideListCards(repo: CardRepository): ListCards =
        ListCards(repo)

    @Provides
    fun provideListStashes(repo: StashRepository): ListStashes =
        ListStashes(repo)

    @Provides
    fun provideGetWalletBalance(repo: WalletRepository): GetWalletBalance =
        GetWalletBalance(repo)

    @Provides
    fun provideGetCardBalance(repo: CardRepository): GetCardBalance =
        GetCardBalance(repo)

    @Provides
    fun provideGetStashBalance(repo: StashRepository): GetStashBalance =
        GetStashBalance(repo)

    @Provides
    fun provideGetCurrentRate(repo: CurrencyRateRepository): GetCurrentRate =
        GetCurrentRate(repo)
}