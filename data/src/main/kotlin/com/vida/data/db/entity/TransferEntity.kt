package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `transfers` table — cross-source money movements.
 *
 * Money is decomposed into [amountMinor] + [amountCurrency] by the mapper (no
 * single-column TypeConverter for Money). The polymorphic source and destination
 * are each stored as three nullable columns — exactly one is non-null per triplet:
 *
 * - WALLET → `*_wallet_id` = 1L (singleton wallet), other two null
 * - CARD   → `*_card_id`   = the card row id, other two null
 * - STASH  → `*_stash_id`  = the stash row id, other two null
 *
 * The "exactly one non-null" invariant is enforced at the application level (domain
 * `Transfer.init` + mapper construction). A SQL-level CHECK cannot be expressed via
 * Room's `@Entity` (same precedent as `ExpenseEntity` in PR #2).
 *
 * The domain `Transfer` model has no `description` field (only [note]); this entity
 * follows the domain contract and stores no `description` column (design #107 §4).
 */
@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["source_wallet_id"], name = "idx_transfers_source_wallet_id"),
        Index(value = ["source_card_id"], name = "idx_transfers_source_card_id"),
        Index(value = ["source_stash_id"], name = "idx_transfers_source_stash_id"),
        Index(value = ["destination_wallet_id"], name = "idx_transfers_destination_wallet_id"),
        Index(value = ["destination_card_id"], name = "idx_transfers_destination_card_id"),
        Index(value = ["destination_stash_id"], name = "idx_transfers_destination_stash_id"),
        Index(value = ["date_time"], name = "idx_transfers_date_time"),
    ],
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "amount_currency") val amountCurrency: String,
    @ColumnInfo(name = "date_time") val dateTime: Long,
    val note: String?,
    @ColumnInfo(name = "source_wallet_id") val sourceWalletId: Long?,
    @ColumnInfo(name = "source_card_id") val sourceCardId: Long?,
    @ColumnInfo(name = "source_stash_id") val sourceStashId: Long?,
    @ColumnInfo(name = "destination_wallet_id") val destinationWalletId: Long?,
    @ColumnInfo(name = "destination_card_id") val destinationCardId: Long?,
    @ColumnInfo(name = "destination_stash_id") val destinationStashId: Long?,
)
