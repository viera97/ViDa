package com.vida.feature.home.cache

import android.content.Context
import android.content.SharedPreferences
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.feature.home.HomeUiState
import com.vida.feature.home.PerSource
import com.vida.feature.home.RecentExpenseItem
import com.vida.feature.home.RecentIncomeItem
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disk cache for the last computed [HomeUiState.Ready].
 *
 * Uses SharedPreferences + `org.json` (built into Android, zero extra
 * dependencies). On cold start, [load] returns an instant snapshot so the
 * dashboard renders immediately; the normal reactive pipeline refreshes it
 * in the background within seconds.
 *
 * The cache key contract is opaque — single-letter keys keep the pref file
 * small since every [save] rewrites all keys atomically.
 */
@Singleton
class DashboardCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Reads the cached dashboard snapshot. Returns `null` on first launch or
     * after [clear]. Call from a background thread — SharedPreferences may
     * do a blocking disk I/O on the very first access per process.
     */
    fun load(): HomeUiState.Ready? {
        val totalMinor = prefs.getLong(K_TOTAL_MINOR, -1L)
        if (totalMinor == -1L) return null
        val totalCurrency = prefs.getString(K_TOTAL_CURRENCY, null) ?: return null
        val sourcesJson = prefs.getString(K_SOURCES, null) ?: return null
        val currencyTotalsJson = prefs.getString(K_CURRENCY_TOTALS, null) ?: return null
        val expensesJson = prefs.getString(K_EXPENSES, null) ?: return null
        val incomesJson = prefs.getString(K_INCOMES, null) ?: return null
        val ratesJson = prefs.getString(K_RATES, null) ?: return null

        val total = Money.fromMinorUnits(totalMinor, Currency.fromCode(totalCurrency))
        val perSource = parseSources(sourcesJson)
        val perCurrencySubtotals = parseCurrencyTotals(currencyTotalsJson)
        val recentExpenses = parseExpenses(expensesJson)
        val recentIncomes = parseIncomes(incomesJson)
        val rates = parseRates(ratesJson)

        return HomeUiState.Ready(
            totalBalance = total,
            perCurrencySubtotals = perCurrencySubtotals,
            perSource = perSource,
            recentExpenses = recentExpenses,
            recentIncomes = recentIncomes,
            rates = rates,
        )
    }

    /**
     * Persists [state] to disk. Called after every successful [HomeUiState.Ready]
     * emission so the cache is always up-to-date.
     */
    fun save(state: HomeUiState.Ready) {
        prefs.edit()
            .putLong(K_TOTAL_MINOR, state.totalBalance.amount.multiply(BigDecimal(100)).toLong())
            .putString(K_TOTAL_CURRENCY, state.totalBalance.currency.code)
            .putString(K_SOURCES, serializeSources(state.perSource))
            .putString(K_CURRENCY_TOTALS, serializeCurrencyTotals(state.perCurrencySubtotals))
            .putString(K_EXPENSES, serializeExpenses(state.recentExpenses))
            .putString(K_INCOMES, serializeIncomes(state.recentIncomes))
            .putString(K_RATES, serializeRates(state.rates))
            .apply()
    }

    /** Clears all cached data. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    // ── Serialization helpers ─────────────────────────────────────────────

    private fun serializeSources(sources: List<PerSource>): String {
        val arr = JSONArray()
        for (s in sources) {
            arr.put(
                JSONObject().apply {
                    put("label", s.label)
                    put("balanceMinor", s.balance.amount.multiply(BigDecimal(100)).toLong())
                    put("balanceCurrency", s.balance.currency.code)
                    put("formatted", s.formatted)
                    put("sourceType", s.sourceType.name)
                    put("sourceId", s.sourceId)
                },
            )
        }
        return arr.toString()
    }

    private fun serializeCurrencyTotals(totals: Map<Currency, Money>): String {
        val obj = JSONObject()
        for ((currency, money) in totals) {
            obj.put(currency.code, money.amount.multiply(BigDecimal(100)).toLong())
        }
        return obj.toString()
    }

    private fun serializeExpenses(items: List<RecentExpenseItem>): String {
        val arr = JSONArray()
        for (e in items) {
            arr.put(
                JSONObject().apply {
                    put("categoryName", e.categoryName)
                    put("formattedAmount", e.formattedAmount)
                    put("sourceLabel", e.sourceLabel)
                    put("relativeDate", e.relativeDate)
                },
            )
        }
        return arr.toString()
    }

    private fun serializeIncomes(items: List<RecentIncomeItem>): String {
        val arr = JSONArray()
        for (i in items) {
            arr.put(
                JSONObject().apply {
                    put("description", i.description)
                    put("formattedAmount", i.formattedAmount)
                    put("sourceLabel", i.sourceLabel)
                    put("relativeDate", i.relativeDate)
                },
            )
        }
        return arr.toString()
    }

    private fun serializeRates(rates: Map<String, BigDecimal>?): String {
        if (rates == null) return "{}"
        val obj = JSONObject()
        for ((key, value) in rates) {
            obj.put(key, value.toDouble())
        }
        return obj.toString()
    }

    // ── Deserialization helpers ───────────────────────────────────────────

    private fun parseSources(json: String): List<PerSource> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            PerSource(
                label = obj.getString("label"),
                balance = Money.fromMinorUnits(
                    minorUnits = obj.getLong("balanceMinor"),
                    currency = Currency.fromCode(obj.getString("balanceCurrency")),
                ),
                formatted = obj.getString("formatted"),
                sourceType = SourceType.valueOf(obj.getString("sourceType")),
                sourceId = obj.getLong("sourceId"),
            )
        }
    }

    private fun parseCurrencyTotals(json: String): Map<Currency, Money> {
        val obj = JSONObject(json)
        val map = mutableMapOf<Currency, Money>()
        for (key in obj.keys()) {
            val currency = Currency.fromCode(key)
            map[currency] = Money.fromMinorUnits(obj.getLong(key), currency)
        }
        return map
    }

    private fun parseExpenses(json: String): List<RecentExpenseItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RecentExpenseItem(
                categoryName = obj.getString("categoryName"),
                formattedAmount = obj.getString("formattedAmount"),
                sourceLabel = obj.getString("sourceLabel"),
                relativeDate = obj.getString("relativeDate"),
            )
        }
    }

    private fun parseIncomes(json: String): List<RecentIncomeItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RecentIncomeItem(
                description = obj.getString("description"),
                formattedAmount = obj.getString("formattedAmount"),
                sourceLabel = obj.getString("sourceLabel"),
                relativeDate = obj.getString("relativeDate"),
            )
        }
    }

    private fun parseRates(json: String): Map<String, BigDecimal>? {
        if (json == "{}") return null
        val obj = JSONObject(json)
        val map = mutableMapOf<String, BigDecimal>()
        for (key in obj.keys()) {
            map[key] = BigDecimal.valueOf(obj.getDouble(key))
        }
        return map
    }

    companion object {
        private const val FILE_NAME = "dashboard_cache"
        private const val K_TOTAL_MINOR = "total_minor"
        private const val K_TOTAL_CURRENCY = "total_currency"
        private const val K_SOURCES = "sources"
        private const val K_CURRENCY_TOTALS = "currency_totals"
        private const val K_EXPENSES = "expenses"
        private const val K_INCOMES = "incomes"
        private const val K_RATES = "rates"
    }
}
