package com.vida.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vida.domain.model.Currency

/**
 * Singleton wallet row. CHECK (id = 1) is enforced at domain level (Wallet.init)
 * and repository level (WalletRepositoryImpl.upsert). SQL-level CHECK is a
 * nice-to-have deferred to a future migration.
 */
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: Long = 1L,
    val currency: Currency,
)
