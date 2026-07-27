package com.vida.feature.recurringexpensemanagement

import com.vida.core.ui.SourceItem
import com.vida.domain.model.Card
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet

/** @deprecated Use [SourceItem] from core instead. */
typealias RecurringSourceItem = SourceItem

/** Builds the [SourceItem] list shown in the picker. */
internal fun List<Wallet>.toWalletSourceItems(): List<SourceItem> = map { wallet ->
    SourceItem(
        id = wallet.id,
        type = SourceType.WALLET,
        label = wallet.name,
        subtitle = null,
        currency = wallet.currency,
    )
}

/** Builds the [SourceItem] list for cards shown in the picker. */
internal fun List<Card>.toCardSourceItems(): List<SourceItem> = map { card ->
    SourceItem(
        id = card.id,
        type = SourceType.CARD,
        label = card.bank,
        subtitle = card.number.masked,
        currency = card.currency,
    )
}

/** Builds the [SourceItem] list for stashes shown in the picker. */
internal fun List<Stash>.toStashSourceItems(): List<SourceItem> = map { stash ->
    SourceItem(
        id = stash.id,
        type = SourceType.STASH,
        label = stash.name,
        subtitle = null,
        currency = stash.currency.code,
    )
}
