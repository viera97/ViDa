package com.vida.data.di

import android.content.Context
import com.vida.data.db.AppDatabase
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.mapper.CardMapper
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
    fun provideCardMapper(): CardMapper = CardMapper

    @Provides
    fun provideStashMapper(): StashMapper = StashMapper

    @Provides
    fun provideWalletMapper(): WalletMapper = WalletMapper
}
