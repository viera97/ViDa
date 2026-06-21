package com.vida.data.di

import android.content.Context
import com.vida.data.db.AppDatabase
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.CategoryDao
import com.vida.data.db.dao.CurrencyRateDao
import com.vida.data.db.dao.ExpenseDao
import com.vida.data.db.dao.RefundDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.CardMapper
import com.vida.data.mapper.CategoryMapper
import com.vida.data.mapper.CurrencyRateMapper
import com.vida.data.mapper.ExpenseMapper
import com.vida.data.mapper.RefundMapper
import com.vida.data.mapper.StashMapper
import com.vida.data.mapper.WalletMapper
import com.vida.data.security.DevPassphraseProvider
import com.vida.data.security.PassphraseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providePassphraseProvider(): PassphraseProvider = DevPassphraseProvider

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext ctx: Context,
        passphraseProvider: PassphraseProvider,
    ): AppDatabase = AppDatabase.create(ctx, passphraseProvider)

    @Provides
    fun provideCardDao(db: AppDatabase): CardDao = db.cardDao()

    @Provides
    fun provideStashDao(db: AppDatabase): StashDao = db.stashDao()

    @Provides
    fun provideWalletDao(db: AppDatabase): WalletDao = db.walletDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideRefundDao(db: AppDatabase): RefundDao = db.refundDao()

    @Provides
    fun provideCurrencyRateDao(db: AppDatabase): CurrencyRateDao = db.currencyRateDao()

    @Provides
    fun provideCardMapper(): CardMapper = CardMapper

    @Provides
    fun provideStashMapper(): StashMapper = StashMapper

    @Provides
    fun provideWalletMapper(): WalletMapper = WalletMapper

    @Provides
    fun provideCategoryMapper(): CategoryMapper = CategoryMapper

    @Provides
    fun provideExpenseMapper(): ExpenseMapper = ExpenseMapper

    @Provides
    fun provideRefundMapper(): RefundMapper = RefundMapper

    @Provides
    fun provideCurrencyRateMapper(): CurrencyRateMapper = CurrencyRateMapper
}
