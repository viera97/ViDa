package com.vida.domain.usecase.statistics

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.statistics.CashFlowPoint
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.IncomeRepository
import java.time.Instant

/**
 * Computes the expense + income time-series trend for the given time range.
 *
 * Merges expense and income period totals into a unified list of [CashFlowPoint],
 * grouped by period bucket (daily, weekly, or monthly depending on [bucketMillis]).
 *
 * Each [CashFlowPoint] carries a per-currency map so that multi-currency periods
 * are accurately represented.
 *
 * @param bucketMillis Bucket size in milliseconds (e.g. 86_400_000L for daily).
 */
class GetCashFlowTrend(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
) {
    suspend operator fun invoke(
        from: Instant,
        to: Instant,
        bucketMillis: Long,
    ): List<CashFlowPoint> {
        val expensePeriods = expenseRepository.getExpenseTotalsByPeriod(from, to, bucketMillis)
        val incomePeriods = incomeRepository.getIncomeTotalsByPeriod(from, to, bucketMillis)

        val expenseMap: Map<Long, Map<String, Long>> = expensePeriods
            .groupBy { it.periodStart }
            .mapValues { (_, totals) -> totals.associate { it.currency to it.totalMinor } }

        val incomeMap: Map<Long, Map<String, Long>> = incomePeriods
            .groupBy { it.periodStart }
            .mapValues { (_, totals) -> totals.associate { it.currency to it.totalMinor } }

        val allPeriodStarts = (expenseMap.keys + incomeMap.keys).sorted()

        return allPeriodStarts.map { periodStart ->
            CashFlowPoint(
                periodStart = Instant.ofEpochMilli(periodStart),
                expenseTotal = expenseMap[periodStart]?.map { (code, minor) ->
                    Currency.fromCode(code) to toMoney(minor, code)
                }?.toMap(),
                incomeTotal = incomeMap[periodStart]?.map { (code, minor) ->
                    Currency.fromCode(code) to toMoney(minor, code)
                }?.toMap(),
            )
        }
    }

    private fun toMoney(totalMinor: Long, currencyCode: String): Money {
        val currency = Currency.fromCode(currencyCode)
        return Money.fromMinorUnits(totalMinor, currency)
    }
}
