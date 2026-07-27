package com.vida.data.di

import android.content.Context
import com.vida.data.db.AppDatabase
import com.vida.data.db.callback.AppDatabaseCallback
import com.vida.data.db.dao.BankDao
import com.vida.data.db.dao.BalanceDao
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.CategoryDao
import com.vida.data.db.dao.CurrencyDao
import com.vida.data.db.dao.CurrencyRateDao
import com.vida.data.db.dao.ExpenseDao
import com.vida.data.db.dao.IncomeDao
import com.vida.data.db.dao.RecurringExpenseDao
import com.vida.data.db.dao.RecurringIncomeDao
import com.vida.data.db.dao.RefundDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.TransferDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.BankMapper
import com.vida.data.mapper.CardMapper
import com.vida.data.mapper.CategoryMapper
import com.vida.data.mapper.CurrencyMapper
import com.vida.data.mapper.CurrencyRateMapper
import com.vida.data.mapper.ExpenseMapper
import com.vida.data.mapper.IncomeMapper
import com.vida.data.mapper.RecurringExpenseMapper
import com.vida.data.mapper.RecurringIncomeMapper
import com.vida.data.mapper.RefundMapper
import com.vida.data.mapper.StashMapper
import com.vida.data.mapper.TransferMapper
import com.vida.data.mapper.WalletMapper
import com.vida.data.repository.TransferOrchestrator
import com.vida.data.security.DevPassphraseProvider
import com.vida.data.security.PassphraseProvider
import com.vida.domain.repository.BankRepository
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.CurrencyRepository
import com.vida.domain.usecase.bank.GetBankColorByName
import com.vida.domain.usecase.bank.ListBanks
import com.vida.domain.usecase.bank.SeedDefaultBanks
import com.vida.domain.usecase.category.SeedDefaultCategories
import com.vida.domain.usecase.currency.AddCurrency
import com.vida.domain.usecase.currency.DeleteCurrency
import com.vida.domain.usecase.currency.GetCurrency
import com.vida.domain.usecase.currency.ListCurrencies
import com.vida.domain.usecase.currency.SeedDefaultCurrencies
import com.vida.domain.usecase.currency.UpdateCurrency
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
    fun provideSeedDefaultCategories(repo: CategoryRepository): SeedDefaultCategories =
        SeedDefaultCategories(repo)

    @Provides
    @Singleton
    fun provideSeedDefaultBanks(repo: BankRepository): SeedDefaultBanks =
        SeedDefaultBanks(repo)

    @Provides
    @Singleton
    fun provideSeedDefaultCurrencies(repo: CurrencyRepository): SeedDefaultCurrencies =
        SeedDefaultCurrencies(repo)

    @Provides
    @Singleton
    fun provideListCurrencies(repo: CurrencyRepository): ListCurrencies =
        ListCurrencies(repo)

    @Provides
    @Singleton
    fun provideGetCurrency(repo: CurrencyRepository): GetCurrency =
        GetCurrency(repo)

    @Provides
    @Singleton
    fun provideAddCurrency(repo: CurrencyRepository): AddCurrency =
        AddCurrency(repo)

    @Provides
    @Singleton
    fun provideUpdateCurrency(repo: CurrencyRepository): UpdateCurrency =
        UpdateCurrency(repo)

    @Provides
    @Singleton
    fun provideDeleteCurrency(repo: CurrencyRepository): DeleteCurrency =
        DeleteCurrency(repo)

    @Provides
    @Singleton
    fun provideGetBankColorByName(repo: BankRepository): GetBankColorByName =
        GetBankColorByName(repo)

    @Provides
    @Singleton
    fun provideListBanks(repo: BankRepository): ListBanks =
        ListBanks(repo)

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext ctx: Context,
        passphraseProvider: PassphraseProvider,
        callback: AppDatabaseCallback,
    ): AppDatabase = AppDatabase.create(ctx, passphraseProvider, callback)

    @Provides
    fun provideBankDao(db: AppDatabase): BankDao = db.bankDao()

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
    fun provideIncomeDao(db: AppDatabase): IncomeDao = db.incomeDao()

    @Provides
    fun provideRefundDao(db: AppDatabase): RefundDao = db.refundDao()

    @Provides
    fun provideCurrencyRateDao(db: AppDatabase): CurrencyRateDao = db.currencyRateDao()

    @Provides
    fun provideCurrencyDao(db: AppDatabase): CurrencyDao = db.currencyDao()

    @Provides
    fun provideTransferDao(db: AppDatabase): TransferDao = db.transferDao()

    @Provides
    fun provideRecurringExpenseDao(db: AppDatabase): RecurringExpenseDao =
        db.recurringExpenseDao()

    @Provides
    fun provideRecurringIncomeDao(db: AppDatabase): RecurringIncomeDao =
        db.recurringIncomeDao()

    @Provides
    fun provideBalanceDao(db: AppDatabase): BalanceDao = db.balanceDao()

    @Provides
    @Singleton
    fun provideTransferOrchestrator(
        db: AppDatabase,
        transferDao: TransferDao,
        walletDao: WalletDao,
        cardDao: CardDao,
        stashDao: StashDao,
        transferMapper: TransferMapper,
    ): TransferOrchestrator = TransferOrchestrator(db, transferDao, walletDao, cardDao, stashDao, transferMapper)

    @Provides
    fun provideCardMapper(): CardMapper = CardMapper

    @Provides
    fun provideStashMapper(): StashMapper = StashMapper

    @Provides
    fun provideWalletMapper(): WalletMapper = WalletMapper

    @Provides
    fun provideBankMapper(): BankMapper = BankMapper

    @Provides
    fun provideCategoryMapper(): CategoryMapper = CategoryMapper

    @Provides
    fun provideExpenseMapper(): ExpenseMapper = ExpenseMapper

    @Provides
    fun provideIncomeMapper(): IncomeMapper = IncomeMapper

    @Provides
    fun provideRefundMapper(): RefundMapper = RefundMapper

    @Provides
    fun provideCurrencyRateMapper(): CurrencyRateMapper = CurrencyRateMapper

    @Provides
    fun provideCurrencyMapper(): CurrencyMapper = CurrencyMapper

    @Provides
    fun provideTransferMapper(): TransferMapper = TransferMapper

    @Provides
    fun provideRecurringExpenseMapper(): RecurringExpenseMapper = RecurringExpenseMapper

    @Provides
    fun provideRecurringIncomeMapper(): RecurringIncomeMapper = RecurringIncomeMapper
}
