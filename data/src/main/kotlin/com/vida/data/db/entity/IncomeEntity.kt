package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `incomes` table.
 *
 * Mirrors [ExpenseEntity] but without `category_id` and `real_amount_*` —
 * incomes are not categorized the way expenses are, and the stored amount IS
 * the actual received amount (no "planned vs real" distinction).
 *
 * Money decomposes into `amount_minor` + `amount_currency` via the mapper
 * utility. The polymorphic destination source is stored as three nullable
 * columns — exactly one is non-null per row:
 * - WALLET income → [destinationWalletId] = the wallet row id (commit 5742918),
 *   others null
 * - CARD income   → [destinationCardId] = the card row id, others null
 * - STASH income  → [destinationStashId] = the stash row id, others null
 *
 * The "exactly one non-null" invariant is enforced at the application level
 * (domain `Income.init` + mapper construction), same as expenses. See
 * [ExpenseEntity] for the rationale.
 */
@Entity(
    tableName = "incomes",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["destination_card_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = StashEntity::class,
            parentColumns = ["id"],
            childColumns = ["destination_stash_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["date_time"], name = "idx_incomes_date_time"),
        Index(value = ["destination_card_id"], name = "idx_incomes_destination_card_id"),
        Index(value = ["destination_stash_id"], name = "idx_incomes_destination_stash_id"),
    ],
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "amount_currency") val amountCurrency: String,
    val description: String,
    @ColumnInfo(name = "date_time") val dateTime: Long,
    val note: String?,
    @ColumnInfo(name = "destination_wallet_id") val destinationWalletId: Long?,
    @ColumnInfo(name = "destination_card_id") val destinationCardId: Long?,
    @ColumnInfo(name = "destination_stash_id") val destinationStashId: Long?,
)
