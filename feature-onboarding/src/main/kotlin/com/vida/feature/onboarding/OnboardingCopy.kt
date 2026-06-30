package com.vida.feature.onboarding

/**
 * Centralized Spanish UI copy for the first-run wizard.
 *
 * Literals live at the call site as Spanish-only strings — no `strings.xml`,
 * no locale branching. Adding a new field literal? Append it here so future
 * i18n is a one-file grep.
 */
object OnboardingCopy {
    // Welcome step
    const val WELCOME_HEADLINE = "¡Bienvenido a ViDa!"
    const val WELCOME_SUBHEAD = "Tu billetera personal, simple y privada."
    const val WELCOME_PRIMARY = "Empezar"
    const val WELCOME_SKIP = "Saltar"
}
