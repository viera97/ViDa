package com.vida.feature.ratemanagement.cache

import android.content.Context
import android.content.SharedPreferences
import com.vida.feature.ratemanagement.InverseRateDisplay
import com.vida.feature.ratemanagement.RateDisplayItem
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disk cache for the last observed [RateDisplayItem] list.
 *
 * Uses SharedPreferences + `org.json` (built into Android, zero extra
 * dependencies). On cold start, [load] returns an instant snapshot so the
 * Rates screen renders immediately; [RateListViewModel] refreshes the data
 * in the background via a one-shot fetch.
 */
@Singleton
class RateListCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Reads the cached rate list. Returns `null` on first launch or after
     * [clear]. Call from a background thread — SharedPreferences may do a
     * blocking disk I/O on the very first access per process.
     */
    fun load(): List<RateDisplayItem>? {
        val json = prefs.getString(K_RATES, null) ?: return null
        return parseRates(json)
    }

    /**
     * Persists [rates] to disk. Called after every successful
     * [RateListUiState.Ready] emission so the cache is always up-to-date.
     */
    fun save(rates: List<RateDisplayItem>) {
        prefs.edit()
            .putString(K_RATES, serializeRates(rates))
            .apply()
    }

    /** Clears cached rate data. Called when the rate list becomes empty. */
    fun clear() {
        prefs.edit().remove(K_RATES).apply()
    }

    // ── Serialization helpers ─────────────────────────────────────────────

    private fun serializeRates(rates: List<RateDisplayItem>): String {
        val arr = JSONArray()
        for (r in rates) {
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("fromCurrency", r.fromCurrency)
                    put("toCurrency", r.toCurrency)
                    put("pairLabel", r.pairLabel)
                    put("rate", r.rate.toDouble())
                    put("rateFormatted", r.rateFormatted)
                    put("provider", r.provider)
                    put("updatedAt", r.updatedAt.toString())
                    put("updatedAtFormatted", r.updatedAtFormatted)
                    put("inverse", r.inverse?.let { inv ->
                        JSONObject().apply {
                            put("id", inv.id)
                            put("fromCurrency", inv.fromCurrency)
                            put("toCurrency", inv.toCurrency)
                            put("rate", inv.rate.toDouble())
                            put("rateFormatted", inv.rateFormatted)
                        }
                    } ?: JSONObject.NULL)
                },
            )
        }
        return arr.toString()
    }

    private fun parseRates(json: String): List<RateDisplayItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RateDisplayItem(
                id = obj.getLong("id"),
                fromCurrency = obj.getString("fromCurrency"),
                toCurrency = obj.getString("toCurrency"),
                pairLabel = obj.getString("pairLabel"),
                rate = BigDecimal.valueOf(obj.getDouble("rate")),
                rateFormatted = obj.getString("rateFormatted"),
                provider = obj.getString("provider"),
                updatedAt = Instant.parse(obj.getString("updatedAt")),
                updatedAtFormatted = obj.getString("updatedAtFormatted"),
                inverse = if (obj.isNull("inverse")) null else {
                    val inv = obj.getJSONObject("inverse")
                    InverseRateDisplay(
                        id = inv.getLong("id"),
                        fromCurrency = inv.getString("fromCurrency"),
                        toCurrency = inv.getString("toCurrency"),
                        rate = BigDecimal.valueOf(inv.getDouble("rate")),
                        rateFormatted = inv.getString("rateFormatted"),
                    )
                },
            )
        }
    }

    companion object {
        private const val FILE_NAME = "rate_list_cache"
        private const val K_RATES = "rates"
    }
}
