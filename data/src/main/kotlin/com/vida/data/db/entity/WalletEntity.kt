package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Wallet row with AUTOINCREMENT PK for multi-wallet support (v2).
 */
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val currency: String = "CUP",
    @ColumnInfo(name = "name") val name: String = "Billetera",
    @ColumnInfo(name = "balance_minor") val balanceMinor: Long = 0,
    @ColumnInfo(name = "initial_balance_currency") val initialBalanceCurrency: String = "CUP",
)
