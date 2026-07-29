package com.vida.feature.walletmanagement.cache

import android.content.Context
import android.content.SharedPreferences
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.feature.walletmanagement.WalletDisplayItem
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disk cache for the last observed [WalletDisplayItem] list.
 *
 * Uses SharedPreferences + `org.json` (built into Android, zero extra
 * dependencies). On cold start, [load] returns an instant snapshot so the
 * Fuentes screen renders immediately; the normal reactive pipeline refreshes
 * it in the background within seconds.
 */
@Singleton
class WalletListCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Reads the cached wallet list. Returns `null` on first launch or after
     * [clear]. Call from a background thread — SharedPreferences may do a
     * blocking disk I/O on the very first access per process.
     */
    fun load(): List<WalletDisplayItem>? {
        val json = prefs.getString(K_WALLETS, null) ?: return null
        return parseWallets(json)
    }

    /**
     * Persists [wallets] to disk. Called after every fresh [WalletListUiState.Ready]
     * emission so the cache is always up-to-date.
     */
    fun save(wallets: List<WalletDisplayItem>) {
        prefs.edit()
            .putString(K_WALLETS, serializeWallets(wallets))
            .apply()
    }

    /** Clears cached wallet data. Called when the wallet list becomes empty. */
    fun clear() {
        prefs.edit().remove(K_WALLETS).apply()
    }

    // ── Serialization helpers ─────────────────────────────────────────────

    private fun serializeWallets(wallets: List<WalletDisplayItem>): String {
        val arr = JSONArray()
        for (w in wallets) {
            arr.put(
                JSONObject().apply {
                    put("id", w.id)
                    put("name", w.name)
                    put("currencyCode", w.currencyCode)
                    put("balanceFormatted", w.balanceFormatted)
                    put("balanceMinor", w.balance.amount.multiply(BigDecimal(100)).toLong())
                    put("balanceCurrency", w.balance.currency.code)
                    put("currency", w.currency)
                },
            )
        }
        return arr.toString()
    }

    private fun parseWallets(json: String): List<WalletDisplayItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            WalletDisplayItem(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                currencyCode = obj.getString("currencyCode"),
                balanceFormatted = obj.getString("balanceFormatted"),
                balance = Money.fromMinorUnits(
                    minorUnits = obj.getLong("balanceMinor"),
                    currency = Currency.fromCode(obj.getString("balanceCurrency")),
                ),
                currency = obj.getString("currency"),
            )
        }
    }

    companion object {
        private const val FILE_NAME = "wallet_list_cache"
        private const val K_WALLETS = "wallets"
    }
}
