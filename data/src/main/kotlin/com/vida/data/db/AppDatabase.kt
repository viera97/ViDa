package com.vida.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vida.data.db.dao.CardDao
import com.vida.data.db.dao.StashDao
import com.vida.data.db.dao.WalletDao
import com.vida.data.db.entity.CardEntity
import com.vida.data.db.entity.StashEntity
import com.vida.data.db.entity.WalletEntity
import com.vida.data.security.PassphraseProvider
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [CardEntity::class, StashEntity::class, WalletEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun stashDao(): StashDao
    abstract fun walletDao(): WalletDao

    companion object {
        fun create(ctx: Context, passphraseProvider: PassphraseProvider): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "vida.db")
                .openHelperFactory(SupportFactory(passphraseProvider.getPassphrase()))
                .build()
    }
}
