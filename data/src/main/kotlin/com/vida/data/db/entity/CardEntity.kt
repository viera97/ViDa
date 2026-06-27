package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import java.time.LocalDate

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val maskedNumber: String,
    val bank: String,
    val type: CardType,
    val currency: Currency,
    val note: String?,
    val expirationDate: LocalDate,
    @ColumnInfo(name = "balance_minor") val balanceMinor: Long = 0,
    @ColumnInfo(name = "initial_balance_currency") val initialBalanceCurrency: String = "CUP",
)
