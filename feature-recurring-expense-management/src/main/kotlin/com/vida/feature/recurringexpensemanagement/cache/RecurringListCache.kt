package com.vida.feature.recurringexpensemanagement.cache

import android.content.Context
import android.content.SharedPreferences
import com.vida.domain.model.SourceType
import com.vida.feature.recurringexpensemanagement.RecurringDisplayItem
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disk cache for the last observed [RecurringDisplayItem] list.
 *
 * Uses SharedPreferences + `org.json` (built into Android, zero extra
 * dependencies). On cold start, [load] returns an instant snapshot so the
 * Recurring screen renders immediately; the normal reactive pipeline refreshes
 * it in the background within seconds.
 */
@Singleton
class RecurringListCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Reads the cached recurring template list. Returns `null` on first
     * launch or after [clear].
     */
    fun load(): List<RecurringDisplayItem>? {
        val json = prefs.getString(K_TEMPLATES, null) ?: return null
        return parseTemplates(json)
    }

    /**
     * Persists [templates] to disk. Called after every fresh
     * [RecurringListUiState.Ready] emission.
     */
    fun save(templates: List<RecurringDisplayItem>) {
        prefs.edit()
            .putString(K_TEMPLATES, serializeTemplates(templates))
            .apply()
    }

    /** Clears cached template data. Called when the list becomes empty. */
    fun clear() {
        prefs.edit().remove(K_TEMPLATES).apply()
    }

    // ── Serialization helpers ─────────────────────────────────────────────

    private fun serializeTemplates(items: List<RecurringDisplayItem>): String {
        val arr = JSONArray()
        for (item in items) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("amountFormatted", item.amountFormatted)
                    put("currencyCode", item.currencyCode)
                    put("categoryName", item.categoryName)
                    put("frequencyLabel", item.frequencyLabel)
                    put("sourceType", item.sourceType.name)
                    put("sourceTypeIcon", item.sourceTypeIcon)
                    put("nextDueFormatted", item.nextDueFormatted)
                    put("description", item.description)
                    put("isActive", item.isActive)
                    put("type", item.type.name)
                    put("frequencyOrdinal", item.frequencyOrdinal)
                    put("startDateEpochDay", item.startDateEpochDay)
                },
            )
        }
        return arr.toString()
    }

    private fun parseTemplates(json: String): List<RecurringDisplayItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            RecurringDisplayItem(
                id = obj.getLong("id"),
                amountFormatted = obj.getString("amountFormatted"),
                currencyCode = obj.getString("currencyCode"),
                categoryName = obj.getString("categoryName"),
                frequencyLabel = obj.getString("frequencyLabel"),
                sourceType = SourceType.valueOf(obj.getString("sourceType")),
                sourceTypeIcon = obj.getString("sourceTypeIcon"),
                nextDueFormatted = obj.getString("nextDueFormatted"),
                description = obj.getString("description"),
                isActive = obj.getBoolean("isActive"),
                type = RecurringDisplayItem.ItemType.valueOf(obj.getString("type")),
                frequencyOrdinal = obj.getInt("frequencyOrdinal"),
                startDateEpochDay = obj.getLong("startDateEpochDay"),
            )
        }
    }

    companion object {
        private const val FILE_NAME = "recurring_list_cache"
        private const val K_TEMPLATES = "templates"
    }
}
