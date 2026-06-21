package com.vida.data.db.entity

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
)
