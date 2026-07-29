package com.vida.feature.cardmanagement.cache

import android.content.Context
import android.content.SharedPreferences
import com.vida.domain.model.CardType
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.feature.cardmanagement.CardDisplayItem
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disk cache for the last observed [CardDisplayItem] list.
 *
 * Uses SharedPreferences + `org.json` (built into Android, zero extra
 * dependencies). On cold start, [load] returns an instant snapshot so the
 * Fuentes screen renders immediately; the normal reactive pipeline refreshes
 * it in the background within seconds.
 */
@Singleton
class CardListCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Reads the cached card list. Returns `null` on first launch or after
     * [clear]. Call from a background thread — SharedPreferences may do a
     * blocking disk I/O on the very first access per process.
     */
    fun load(): List<CardDisplayItem>? {
        val json = prefs.getString(K_CARDS, null) ?: return null
        return parseCards(json)
    }

    /**
     * Persists [cards] to disk. Called after every fresh [CardListUiState.Ready]
     * emission so the cache is always up-to-date.
     */
    fun save(cards: List<CardDisplayItem>) {
        prefs.edit()
            .putString(K_CARDS, serializeCards(cards))
            .apply()
    }

    /** Clears cached card data. Called when the card list becomes empty. */
    fun clear() {
        prefs.edit().remove(K_CARDS).apply()
    }

    // ── Serialization helpers ─────────────────────────────────────────────

    private fun serializeCards(cards: List<CardDisplayItem>): String {
        val arr = JSONArray()
        for (c in cards) {
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("formattedNumber", c.formattedNumber)
                    put("first6", c.first6)
                    put("last4", c.last4)
                    put("bank", c.bank)
                    put("type", c.type.name)
                    put("currency", c.currency)
                    put("expiryFormatted", c.expiryFormatted)
                    put("expiry", c.expiry.toString())
                    put("note", c.note ?: JSONObject.NULL)
                    put("balanceFormatted", c.balanceFormatted)
                    put("balanceMinor", c.balance.amount.multiply(BigDecimal(100)).toLong())
                    put("balanceCurrency", c.balance.currency.code)
                },
            )
        }
        return arr.toString()
    }

    private fun parseCards(json: String): List<CardDisplayItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            CardDisplayItem(
                id = obj.getLong("id"),
                formattedNumber = obj.getString("formattedNumber"),
                first6 = obj.getString("first6"),
                last4 = obj.getString("last4"),
                bank = obj.getString("bank"),
                type = CardType.valueOf(obj.getString("type")),
                currency = obj.getString("currency"),
                expiryFormatted = obj.getString("expiryFormatted"),
                expiry = LocalDate.parse(obj.getString("expiry")),
                note = if (obj.isNull("note")) null else obj.getString("note"),
                balanceFormatted = obj.getString("balanceFormatted"),
                balance = Money.fromMinorUnits(
                    minorUnits = obj.getLong("balanceMinor"),
                    currency = Currency.fromCode(obj.getString("balanceCurrency")),
                ),
            )
        }
    }

    companion object {
        private const val FILE_NAME = "card_list_cache"
        private const val K_CARDS = "cards"
    }
}
