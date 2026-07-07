package com.vida.feature.onboarding.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed preference flag tracking whether the user has completed the
 * first-run wizard. Owns its own [DataStore] instance via a top-level
 * [Context.onboardingDataStore] delegate so the store never collides with
 * another module's preferences file.
 *
 * File name is "vida_onboarding" — if a future feature needs its own
 * DataStore, rename this to "vida_onboarding_wizard" first to avoid the
 * "two `preferencesDataStore` delegates on the same file" runtime crash.
 *
 * @property dataStore Underlying preferences store, provided by Hilt as a
 *   `@Singleton` via [com.vida.feature.onboarding.di.OnboardingModule].
 */
class WizardPreferences(private val dataStore: DataStore<Preferences>) {

    /** Emits the persisted `wizard_completed` flag. Defaults to `false` when the key is absent. */
    val wizardCompleted: Flow<Boolean> =
        dataStore.data.map { it[KEY_WIZARD_COMPLETED] ?: false }

    /** Sets the `wizard_completed` flag. Suspending — awaits the disk write. */
    suspend fun setWizardCompleted(value: Boolean) {
        dataStore.edit { it[KEY_WIZARD_COMPLETED] = value }
    }

    companion object {
        val KEY_WIZARD_COMPLETED = booleanPreferencesKey("wizard_completed")
        internal const val DATASTORE_NAME = "vida_onboarding"
    }
}

/**
 * Top-level [Context] extension that lazily creates the singleton DataStore
 * for wizard preferences. Only one delegate per process is allowed per
 * [WizardPreferences.DATASTORE_NAME]. `internal` so [OnboardingModule] can
 * resolve the same instance via the same delegate.
 */
internal val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = WizardPreferences.DATASTORE_NAME,
)
