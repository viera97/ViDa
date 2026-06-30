package com.vida.feature.transfermanagement.di

import com.vida.domain.repository.TransferRepository
import com.vida.domain.usecase.transfer.RecordTransfer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module providing the transfer-form-specific use case.
 *
 * NOTE: [com.vida.domain.usecase.card.ListCards] and
 * [com.vida.domain.usecase.stash.ListStashes] are already provided by
 * [com.vida.feature.home.di.HomeModule] in the same [ViewModelComponent].
 *
 * NOTE: [com.vida.domain.usecase.wallet.GetWallet] is already provided by
 * [com.vida.feature.expense.di.ExpenseModule] in the same [ViewModelComponent].
 *
 * Duplicating any of these bindings would cause a Dagger compilation error.
 */
@Module
@InstallIn(ViewModelComponent::class)
object TransferModule {

    @Provides
    fun provideRecordTransfer(repo: TransferRepository): RecordTransfer =
        RecordTransfer(repo)
}
