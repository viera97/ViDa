package com.vida.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.CategoryDao
import com.vida.data.db.dao.CurrencyRateDao
import com.vida.data.db.dao.ExpenseDao
import com.vida.data.db.dao.RefundDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.db.entity.CardEntity
import com.vida.data.db.entity.CategoryEntity
import com.vida.data.db.entity.CurrencyRateEntity
import com.vida.data.db.entity.ExpenseEntity
import com.vida.data.db.entity.RefundEntity
import com.vida.data.db.entity.StashEntity
import com.vida.data.db.entity.WalletEntity
import com.vida.data.security.PassphraseProvider
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        CardEntity::class,
        StashEntity::class,
        WalletEntity::class,
        CategoryEntity::class,
        ExpenseEntity::class,
        RefundEntity::class,
        CurrencyRateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun stashDao(): StashDao
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun refundDao(): RefundDao
    abstract fun currencyRateDao(): CurrencyRateDao

    companion object {
        fun create(ctx: Context, passphraseProvider: PassphraseProvider): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "vida.db")
                .openHelperFactory(SupportFactory(passphraseProvider.getPassphrase()))
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
