package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `expenses` table.
 *
 * Money is decomposed into two columns ([amountMinor] + [amountCurrency]) by the mapper;
 * there is no single-column TypeConverter for Money. The polymorphic source is stored as
 * three nullable columns — exactly one is non-null per row:
 * - WALLET expense → [sourceWalletId] = 1L (singleton wallet), others null
 * - CARD expense   → [sourceCardId] = the card row id, others null
 * - STASH expense  → [sourceStashId] = the stash row id, others null
 *
 * The "exactly one non-null" invariant is enforced at the application level (domain
 * `Expense.init` + mapper construction). A SQL-level CHECK cannot be expressed via Room's
 * `@Entity` and adding it only in the migration would break `MigrationTestHelper` schema
 * validation; see vida-data design #107 §4 (WalletEntity precedent).
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_card_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = StashEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_stash_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["category_id"], name = "idx_expenses_category_id"),
        Index(value = ["date_time"], name = "idx_expenses_date_time"),
        Index(value = ["source_card_id"], name = "idx_expenses_source_card_id"),
        Index(value = ["source_stash_id"], name = "idx_expenses_source_stash_id"),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "amount_currency") val amountCurrency: String,
    @ColumnInfo(name = "real_amount_minor") val realAmountMinor: Long?,
    @ColumnInfo(name = "real_amount_currency") val realAmountCurrency: String?,
    val description: String,
    @ColumnInfo(name = "date_time") val dateTime: Long,
    val note: String?,
    @ColumnInfo(name = "source_wallet_id") val sourceWalletId: Long?,
    @ColumnInfo(name = "source_card_id") val sourceCardId: Long?,
    @ColumnInfo(name = "source_stash_id") val sourceStashId: Long?,
)
