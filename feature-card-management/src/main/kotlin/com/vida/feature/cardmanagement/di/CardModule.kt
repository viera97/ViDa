package com.vida.feature.cardmanagement.di

import com.vida.domain.repository.CardRepository
import com.vida.domain.usecase.card.AddCard
import com.vida.domain.usecase.card.DeleteCard
import com.vida.domain.usecase.card.GetCard
import com.vida.domain.usecase.card.UpdateCard
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing card-specific use cases for ViewModels.
 *
 * NOTE: [com.vida.domain.usecase.card.ListCards] is NOT provided here —
 * it is already provided by [com.vida.feature.expense.di.ExpenseModule] in the
 * same [ViewModelComponent]. Duplicating it would cause a Dagger binding error.
 */
@Module
@InstallIn(ViewModelComponent::class)
object CardModule {

    @Provides
    fun provideAddCard(repo: CardRepository): AddCard =
        AddCard(repo)

    @Provides
    fun provideUpdateCard(repo: CardRepository): UpdateCard =
        UpdateCard(repo)

    @Provides
    fun provideDeleteCard(repo: CardRepository): DeleteCard =
        DeleteCard(repo)

    @Provides
    fun provideGetCard(repo: CardRepository): GetCard =
        GetCard(repo)
}
