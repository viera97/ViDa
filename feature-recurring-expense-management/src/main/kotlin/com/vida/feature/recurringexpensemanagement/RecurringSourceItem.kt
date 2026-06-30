package com.vida.feature.recurringexpensemanagement

import com.vida.domain.model.Card
import com.vida.domain.model.Currency
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet

/**
 * A single source (wallet or card) as displayed in the recurring expense
 * source picker.
 *
 * Intentionally a separate type from `feature-expense`'s `SourceItem`: this
 * picker does NOT include stashes (recurring expenses only support wallet
 * and card sources).
 *
 * @property id Entity id.
 * @property type Which kind of source this is ([SourceType.WALLET] or
 *   [SourceType.CARD]).
 * @property label Display name (e.g. "Billetera principal", "Banco kubo").
 * @property subtitle Optional supplementary text (e.g. masked card number).
 * @property currency The source's native currency.
 */
data class RecurringSourceItem(
    val id: Long?,
    val type: SourceType,
    val label: String,
    val subtitle: String? = null,
    val currency: Currency,
)

/** Builds the [RecurringSourceItem] list shown in the picker. */
internal fun List<Wallet>.toWalletSourceItems(): List<RecurringSourceItem> = map { wallet ->
    RecurringSourceItem(
        id = wallet.id,
        type = SourceType.WALLET,
        label = wallet.name,
        subtitle = null,
        currency = wallet.currency,
    )
}

/** Builds the [RecurringSourceItem] list for cards shown in the picker. */
internal fun List<Card>.toCardSourceItems(): List<RecurringSourceItem> = map { card ->
    RecurringSourceItem(
        id = card.id,
        type = SourceType.CARD,
        label = card.bank,
        subtitle = card.number.masked,
        currency = card.currency,
    )
}

/** Builds the [RecurringSourceItem] list for stashes shown in the picker. */
internal fun List<Stash>.toStashSourceItems(): List<RecurringSourceItem> = map { stash ->
    RecurringSourceItem(
        id = stash.id,
        type = SourceType.STASH,
        label = stash.name,
        subtitle = null,
        currency = stash.currency,
    )
}
