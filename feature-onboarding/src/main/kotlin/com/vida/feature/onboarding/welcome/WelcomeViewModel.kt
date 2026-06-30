package com.vida.feature.onboarding.welcome

import androidx.lifecycle.ViewModel
import com.vida.feature.onboarding.preferences.WizardPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the wizard's welcome step. Exposes a single suspending
 * [markCompleted] write that flips DataStore's `wizard_completed` flag to
 * `true` — used by both the "Saltar" affordance and the hardware-back
 * intercept.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val wizardPreferences: WizardPreferences,
) : ViewModel() {

    /** Awaits the DataStore write. Callers should `launch` and wait before navigating. */
    suspend fun markCompleted() {
        wizardPreferences.setWizardCompleted(true)
    }
}
