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

    // Wallet-or-card step
    const val WOC_HEADLINE = "Crea tu primera fuente"
    const val WOC_SUBHEAD = "¿Qué quieres registrar?"
    const val WOC_SEGMENT_WALLET = "Billetera"
    const val WOC_SEGMENT_CARD = "Tarjeta"
    const val WOC_FIELD_NAME = "Nombre"
    const val WOC_FIELD_CURRENCY = "Moneda"
    const val WOC_FIELD_WALLET_BALANCE = "Saldo inicial"
    const val WOC_FIELD_CARD_BALANCE = "Saldo"
    const val WOC_PRIMARY = "Continuar"
    const val WOC_SKIP = "Saltar"
    const val WOC_ERR_NAME_BLANK = "El nombre es obligatorio"
    const val WOC_ERR_NAME_LONG = "Máximo 100 caracteres"
    const val WOC_ERR_BALANCE_PARSE = "Saldo inválido"

    // Get-started step
    const val GS_HEADLINE = "¡Todo listo!"
    const val GS_BODY =
        "Ya podés registrar gastos, ingresos y transferir entre tus fuentes desde la pantalla principal."
    const val GS_PRIMARY = "Ir al inicio"
    const val GS_SKIP = "Saltar"
}
