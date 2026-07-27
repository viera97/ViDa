package com.vida.data.db.callback

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vida.domain.usecase.bank.SeedDefaultBanks
import com.vida.domain.usecase.category.SeedDefaultCategories
import com.vida.domain.usecase.currency.SeedDefaultCurrencies
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Room `Callback` that seeds default categories on first database creation.
 *
 * Room's `onCreate` fires during `Room.databaseBuilder().build()`, which happens
 * before Hilt has fully initialized the singleton graph on cold start. This callback
 * uses [EntryPointAccessors] to lazily retrieve [SeedDefaultCategories] from the
 * Hilt graph inside a coroutine on `Dispatchers.IO` — by then, Hilt is ready.
 *
 * The seed is idempotent (`SeedDefaultCategories.invoke()` checks if any rows exist
 * and returns early). If the seed fails (e.g., Hilt injection unavailable), the
 * error is logged and swallowed — the app continues without categories.
 * `AddExpenseUseCase` handles missing-category errors gracefully.
 *
 * `onCreate` fires ONLY on fresh creation, never on migration — so the seed does
 * not run on upgrade (SCN-DATA-PR3-014).
 */
class AppDatabaseCallback @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : RoomDatabase.Callback() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SeedEntryPoint {
        fun seedDefaultCategories(): SeedDefaultCategories
        fun seedDefaultBanks(): SeedDefaultBanks
        fun seedDefaultCurrencies(): SeedDefaultCurrencies
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    SeedEntryPoint::class.java,
                )
                entryPoint.seedDefaultCategories().invoke()
                entryPoint.seedDefaultBanks().invoke()
                entryPoint.seedDefaultCurrencies().invoke()
            } catch (e: Exception) {
                Log.w("AppDatabaseCallback", "Failed to seed default categories", e)
            }
        }
    }
}
