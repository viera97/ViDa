package com.vida.domain.model

/**
 * Cadence at which a recurring expense generates a new [Expense] row.
 *
 * Used by PR #2b's `RecurringExpense` entity; defined here in PR #2a so the
 * date-math model lives in one place.
 */
enum class Frequency { DAILY, WEEKLY, MONTHLY, YEARLY }