package com.vida.domain.usecase.statistics

import com.vida.domain.model.Category
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.aggregate.PeriodCategoryExpenseTotal
import com.vida.domain.model.aggregate.PeriodExpenseTotal
import com.vida.domain.model.aggregate.PeriodIncomeTotal
import com.vida.domain.model.statistics.CategoryBreakdown
import com.vida.domain.model.statistics.PeriodReportEntry
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Computes the per-bucket report data for the Reports screen.
 *
 * Joins three period sources (expenses-by-category, expenses-by-period, incomes-by-period)
 * plus a one-shot snapshot of category metadata, into a list of [PeriodReportEntry]
 * sorted by `periodStart` ASCENDING.
 *
 * Each entry carries:
 * - `categoryBreakdown` — per-(category, currency) expense rows for the bucket
 * - `incomeByCurrency` — aggregated income per currency (empty when no income)
 * - `expenseByCurrency` — aggregated expense per currency (empty when no expense)
 * - `netByCurrency` — per-currency net, computed as `income - expense`. The missing side
 *   of any currency is reconstructed as [Money.fromMinorUnits] with `0L` so that
 *   [Money.minus] is never invoked across currencies.
 *
 * Buckets with NO transactions on either side are OMITTED from the output (the union of
 * `periodStart` keys across the three sources never contains a "ghost" bucket).
 *
 * NOTE: `categoryRepository.getAll().first()` is a one-shot snapshot. Categories added
 * mid-session do not propagate unless the user toggles a granularity chip to force a
 * reload — this matches [GetCategoryBreakdown] behavior.
 *
 * @param bucketMillis Bucket size in milliseconds matching the SQL `GROUP BY` bucket.
 */
class GetPeriodReports(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(
        from: Instant,
        to: Instant,
        bucketMillis: Long,
    ): List<PeriodReportEntry> {
        val expensesByCatPeriod = expenseRepository.getExpenseCategoryTotalsByPeriod(from, to, bucketMillis)
        val expensesByPeriod = expenseRepository.getExpenseTotalsByPeriod(from, to, bucketMillis)
        val incomesByPeriod = incomeRepository.getIncomeTotalsByPeriod(from, to, bucketMillis)
        val categories: Map<Long, Category> = categoryRepository.getAll().first().associateBy { it.id }

        val expensesByCatByStart: Map<Long, List<PeriodCategoryExpenseTotal>> =
            expensesByCatPeriod.groupBy { it.periodStart }
        val expensesByPeriodByStart: Map<Long, List<PeriodExpenseTotal>> =
            expensesByPeriod.groupBy { it.periodStart }
        val incomesByPeriodByStart: Map<Long, List<PeriodIncomeTotal>> =
            incomesByPeriod.groupBy { it.periodStart }

        val allStarts: Set<Long> =
            expensesByCatByStart.keys + expensesByPeriodByStart.keys + incomesByPeriodByStart.keys

        return allStarts.sorted().map { periodStartLong ->
            val periodStart: Instant = Instant.ofEpochMilli(periodStartLong)

            val categoryBreakdown: List<CategoryBreakdown> = expensesByCatByStart[periodStartLong]
                .orEmpty()
                .groupBy { it.categoryId }
                .flatMap { (categoryId, rows) ->
                    rows.map { row ->
                        val currency = Currency.fromCode(row.currency)
                        val category = categories[categoryId]
                        CategoryBreakdown(
                            categoryId = categoryId,
                            categoryName = category?.name ?: "Unknown",
                            color = category?.color ?: 0,
                            icon = category?.icon,
                            total = Money.fromMinorUnits(row.totalMinor, currency),
                        )
                    }
                }
                .sortedByDescending { it.total.amount }

            val expenseByCurrency: Map<Currency, Money> = expensesByPeriodByStart[periodStartLong]
                .orEmpty()
                .associate { row ->
                    val c = Currency.fromCode(row.currency)
                    c to Money.fromMinorUnits(row.totalMinor, c)
                }

            val incomeByCurrency: Map<Currency, Money> = incomesByPeriodByStart[periodStartLong]
                .orEmpty()
                .associate { row ->
                    val c = Currency.fromCode(row.currency)
                    c to Money.fromMinorUnits(row.totalMinor, c)
                }

            val allCurrencies = incomeByCurrency.keys + expenseByCurrency.keys
            val netByCurrency: Map<Currency, Money> = allCurrencies.associateWith { c ->
                val inc = incomeByCurrency[c] ?: Money.fromMinorUnits(0L, c)
                val exp = expenseByCurrency[c] ?: Money.fromMinorUnits(0L, c)
                inc.minus(exp)
            }

            PeriodReportEntry(
                periodStart = periodStart,
                categoryBreakdown = categoryBreakdown,
                incomeByCurrency = incomeByCurrency,
                expenseByCurrency = expenseByCurrency,
                netByCurrency = netByCurrency,
            )
        }
    }
}
