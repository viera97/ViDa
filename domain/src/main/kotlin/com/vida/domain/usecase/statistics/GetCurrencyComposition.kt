package com.vida.domain.usecase.statistics

import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.statistics.CurrencyComposition
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.IncomeRepository
import java.time.Instant

/**
 * Computes expense and income totals grouped by currency for the given time range.
 *
 * Merges expense and income currency totals into a unified list of [CurrencyComposition],
 * one entry per currency present in either expenses or incomes.
 */
class GetCurrencyComposition(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
) {
    suspend operator fun invoke(from: Instant, to: Instant): List<CurrencyComposition> {
        val expenseTotals = expenseRepository.getExpenseTotalsByCurrency(from, to)
        val incomeTotals = incomeRepository.getIncomeTotalsByCurrency(from, to)

        val expenseMap = expenseTotals.associateBy { it.currency }
        val incomeMap = incomeTotals.associateBy { it.currency }

        val allCurrencies = (expenseMap.keys + incomeMap.keys).sorted()

        return allCurrencies.map { currencyCode ->
            val currency = Currency.fromCode(currencyCode)
            CurrencyComposition(
                currency = currency,
                expenseTotal = expenseMap[currencyCode]?.let {
                    toMoney(it.totalMinor, currency)
                },
                incomeTotal = incomeMap[currencyCode]?.let {
                    toMoney(it.totalMinor, currency)
                },
            )
        }
    }

    private fun toMoney(totalMinor: Long, currency: Currency): Money =
        Money.fromMinorUnits(totalMinor, currency)
}
