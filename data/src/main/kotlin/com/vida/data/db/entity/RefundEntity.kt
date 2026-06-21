package com.vida.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `refunds` table.
 *
 * A UNIQUE index on [originalExpenseId] enforces one-refund-per-expense at the SQL level.
 * The repository catches the resulting constraint exception and re-throws as
 * `IllegalStateException`. ON DELETE CASCADE removes a refund when its expense is deleted.
 */
@Entity(
    tableName = "refunds",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["original_expense_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["original_expense_id"], unique = true, name = "idx_refunds_original_expense_id"),
    ],
)
data class RefundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "original_expense_id") val originalExpenseId: Long,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "amount_currency") val amountCurrency: String,
    val reason: String,
    @ColumnInfo(name = "date_time") val dateTime: Long,
    val note: String?,
)
