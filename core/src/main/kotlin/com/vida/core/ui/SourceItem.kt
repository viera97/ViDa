package com.vida.core.ui

import com.vida.domain.model.SourceType

/**
 * A single source (wallet, card, or stash) displayed in source pickers.
 *
 * @property id Entity id.
 * @property type Which kind of source this is.
 * @property label Display name (e.g. "Billetera", "Banco kubo", "Ahorro vacaciones").
 * @property subtitle Optional supplementary text (e.g. masked card number).
 * @property currency The source's native currency code (raw string, e.g. "BOB").
 */
data class SourceItem(
    val id: Long?,
    val type: SourceType,
    val label: String,
    val subtitle: String? = null,
    val currency: String,
)
