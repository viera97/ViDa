package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `recurring_expenses` table — templates for recurring expenses.
 *
 * Money is decomposed into [amountMinor] + [amountCurrency] by the mapper. The
 * polymorphic source follows the same triplet pattern as `ExpenseEntity`.
 *
 * [frequency] is stored as TEXT (enum name) via `FrequencyConverter` (shipped in PR #2).
 * [isActive] is stored as INTEGER (1/0). [startDate], [endDate], and [lastGeneratedDate]
 * are stored as epoch-day integers (LocalDate).
 *
 * The domain `RecurringExpense` has a separate `currency` field that always equals
 * `amount.currency`; this entity does NOT store a redundant `currency_code` column —
 * the mapper reconstructs `currency` from `amount_currency` (minor deviation from
 * design #107 §4 which listed a denormalized `currency_code`; the domain invariant
 * guarantees they match). The domain model also has no `intervalCount` field, so no
 * `interval_count` column is stored (design #107 §4 explicitly defers this).
 */
@Entity(
    tableName = "recurring_expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["category_id"], name = "idx_recurring_expenses_category_id"),
        Index(value = ["source_card_id"], name = "idx_recurring_expenses_source_card_id"),
        Index(value = ["source_stash_id"], name = "idx_recurring_expenses_source_stash_id"),
        Index(value = ["is_active"], name = "idx_recurring_expenses_is_active"),
    ],
)
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "category_id") val categoryId: Long,
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
