package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `recurring_incomes` table — templates for recurring incomes.
 *
 * Money is decomposed into [amountMinor] + [amountCurrency] by the mapper. The
 * polymorphic source follows the same triplet pattern as `IncomeEntity`.
 *
 * [frequency] is stored as TEXT (enum name) via `FrequencyConverter`.
 * [isActive] is stored as INTEGER (1/0). [startDate], [endDate], and [lastGeneratedDate]
 * are stored as epoch-day integers (LocalDate).
 *
 * Unlike [RecurringExpenseEntity], this entity has NO `category_id` column —
 * incomes are not categorized.
 */
@Entity(
    tableName = "recurring_incomes",
    indices = [
        Index(value = ["source_card_id"], name = "idx_recurring_incomes_source_card_id"),
        Index(value = ["source_stash_id"], name = "idx_recurring_incomes_source_stash_id"),
        Index(value = ["is_active"], name = "idx_recurring_incomes_is_active"),
    ],
)
data class RecurringIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "amount_currency") val amountCurrency: String,
    val description: String,
    val frequency: String,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long?,
    @ColumnInfo(name = "last_generated_date") val lastGeneratedDate: Long?,
    @ColumnInfo(name = "is_active") val isActive: Int,
    @ColumnInfo(name = "source_wallet_id") val sourceWalletId: Long?,
    @ColumnInfo(name = "source_card_id") val sourceCardId: Long?,
    @ColumnInfo(name = "source_stash_id") val sourceStashId: Long?,
)
