package com.vida.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Card
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import kotlin.comparisons.nullsLast
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.usecase.balance.GetTotalBalance
import com.vida.domain.usecase.card.GetCardBalance
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.expense.ListExpenses
import com.vida.domain.usecase.income.ListIncomes
import com.vida.domain.usecase.rate.GetCurrentRate
import com.vida.domain.usecase.rate.ListCurrencyRates
import com.vida.domain.usecase.stash.GetStashBalance
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWalletBalance
import com.vida.domain.usecase.wallet.ListWallets
import com.vida.core.format.toRelativeDateString
import com.vida.feature.home.home.formatHomeMoney
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Home dashboard ViewModel. Manages [HomeUiState] transitions driven by the
 * trigger flow (D8 workaround — see design doc).
 *
 * The trigger flow is a [combine] of [ListExpenses], [ListCards], and [ListStashes].
 * On every emission, all balance + rate suspend calls are re-invoked to produce
 * a fresh [HomeUiState.Ready] or [HomeUiState.Empty].
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class HomeViewModel @javax.inject.Inject constructor(
    private val getTotalBalance: GetTotalBalance,
    private val listExpenses: ListExpenses,
    private val listIncomes: ListIncomes,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val getWalletBalance: GetWalletBalance,
    private val getCardBalance: GetCardBalance,
    private val getStashBalance: GetStashBalance,
    private val listWallets: ListWallets,
    private val getCurrentRate: GetCurrentRate,
    private val listCurrencyRates: ListCurrencyRates,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Trigger flow: any change to expenses, incomes, cards, or stashes causes a
     * re-derivation of the full dashboard state.
     *
     * Expenses + incomes are nested in a single combine first so the outer
     * combine stays at the 5-arg overload ceiling — adding a 6th peer
     * (incomes) without nesting would require the vararg overload and lose
     * type inference.
     */
    private val triggerFlow: Flow<Unit> = combine(
        combine(listExpenses(), listIncomes()) { _, _ -> Unit },
        listCards(),
        listStashes(),
        listWallets(),
        listCurrencyRates(),
    ) { _, _, _, _, _ -> Unit }

    init {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            triggerFlow.collect {
                _uiState.value = computeState()
            }
        }
    }

    /**
     * Computes the full dashboard state atomically in a single suspend fun.
     *
     * Balance calls are fail-fast (S2): any throw propagates to [HomeUiState.Error].
     * Rate calls are graceful (S3): a throw hides the rates section.
     * The all-zero check (S8) determines whether to emit [HomeUiState.Empty] or [HomeUiState.Ready].
     *
     * Balance use cases return `Flow<Money>`; we `.first()` them here because
     * [triggerFlow] already re-derives [computeState] whenever the source lists
     * change, so a transfer that modifies a wallet/card/stash's underlying
     * tables will be picked up on the next emission.
     */
    private suspend fun computeState(): HomeUiState {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()

        // ── Per-source balances (fail-fast per S2) ─────────────────────────
        val wallets = listWallets().first()
        val walletBalances = wallets.map { wallet ->
            val balance = try {
                getWalletBalance(wallet.id).first()
            } catch (_: Exception) {
                return HomeUiState.Error("Error de balance")
            }
            wallet to balance
        }

        val cards: List<Card> = listCards().first()
        val cardBalances = cards.map { card ->
            val balance = try {
                getCardBalance(card.id).first()
            } catch (_: Exception) {
                return HomeUiState.Error("Error de tarjeta")
            }
            card to balance
        }

        val stashes: List<Stash> = listStashes().first()
        val stashBalances = stashes.map { stash ->
            val balance = try {
                getStashBalance(stash.id).first()
            } catch (_: Exception) {
                return HomeUiState.Error("Error de ahorro")
            }
            stash to balance
        }

        // ── Per-source list ───────────────────────────────────────────────
        val perSource = mutableListOf<PerSource>()
        for ((wallet, balance) in walletBalances) {
            perSource.add(
                PerSource(
                    label = wallet.name,
                    balance = balance,
                    formatted = formatHomeMoney(balance),
                    sourceType = SourceType.WALLET,
                    sourceId = wallet.id,
                ),
            )
        }
        for ((card, balance) in cardBalances) {
            perSource.add(
                PerSource(
                    // Show the user-entered "card name" (stored as note today) when present,
                    // otherwise fall back to the bank. Cards still don't have a real `name`
                    // field — this is a workaround until the Card model is migrated.
                    label = card.note?.takeIf { it.isNotBlank() } ?: card.bank,
                    balance = balance,
                    formatted = formatHomeMoney(balance),
                    sourceType = SourceType.CARD,
                    sourceId = card.id,
                ),
            )
        }
        for ((stash, balance) in stashBalances) {
            perSource.add(
                PerSource(
                    label = stash.name,
                    balance = balance,
                    formatted = formatHomeMoney(balance),
                    sourceType = SourceType.STASH,
                    sourceId = stash.id,
                ),
            )
        }

        // ── Per-currency roll-up (S4: only non-zero currencies) ───────────
        val perCurrencySubtotals: Map<Currency, Money> = perSource
            .filter { it.balance.amount.signum() > 0 }
            .groupBy({ it.balance.currency }) { it.balance }
            .mapValues { (_, moneys) -> moneys.reduce { acc, m -> acc + m } }

        // ── Total balance ──────────────────────────────────────────────────
        val total = try {
            getTotalBalance()
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            return HomeUiState.Error(t.message ?: "Error de balance")
        }

        // ── Recent expenses (≤5, newest first) ─────────────────────────────
        // Load the FULL expense and income lists once — used both for the
        // recent-rows display AND for the per-source last-use sort below.
        val allExpenses = listExpenses().first()
        val expenses = allExpenses.take(5)

        // ── Recent incomes (≤5, newest first) ──────────────────────────────
        val allIncomes = listIncomes().first()
        val incomes = allIncomes.take(5)

        // ── Per-source last-use map (for "most recently used" ordering) ───
        // Key = "${sourceType}:${sourceId}". Value = max date across expenses
        // and incomes for that source. Sources without transactions are
        // absent from the map and fall to the end of the sort.
        val lastUseBySource: Map<String, Instant> = buildMap {
            for (e in allExpenses) {
                val key = "${e.sourceType.name}:${e.sourceId}"
                val existing = this[key]
                if (existing == null || e.dateTime > existing) {
                    this[key] = e.dateTime
                }
            }
            for (i in allIncomes) {
                val key = "${i.sourceType.name}:${i.sourceId}"
                val existing = this[key]
                if (existing == null || i.dateTime > existing) {
                    this[key] = i.dateTime
                }
            }
        }

        // ── Rates (graceful degradation per S3) ────────────────────────────
        val rates: Map<String, java.math.BigDecimal>? = try {
            val usdRate = getCurrentRate(Currency.USD, Currency.CUP, now)
            val mlcRate = getCurrentRate(Currency.MLC, Currency.CUP, now)
            mapOf(
                "USD" to usdRate.rate,
                "MLC" to mlcRate.rate,
            )
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            null // graceful degradation — section hidden on error
        }

        // ── All-zero check (S8) ─────────────────────────────────────────────
        val allZero = total.isZero() &&
            expenses.isEmpty() &&
            incomes.isEmpty() &&
            (rates == null || rates.isEmpty())

        if (allZero) return HomeUiState.Empty

        // ── Recent expense items ───────────────────────────────────────────
        val categoryMap = runCatching {
            categoryRepository.getAll().first().associate { it.id to it.name }
        }.getOrDefault(emptyMap())

        val recentExpenses = expenses.map { expense ->
            val catName = categoryMap[expense.categoryId] ?: "Sin categoría"
            RecentExpenseItem(
                categoryName = catName,
                formattedAmount = formatHomeMoney(expense.amount),
                sourceLabel = sourceLabelOf(expense.sourceType, expense.sourceId, wallets, cards, stashes),
                relativeDate = expense.dateTime.toString().toRelativeDateString(now, zone),
            )
        }

        // ── Recent income items ────────────────────────────────────────────
        val recentIncomes = incomes.map { income ->
            RecentIncomeItem(
                description = income.description,
                formattedAmount = formatHomeMoney(income.amount),
                sourceLabel = sourceLabelOf(income.sourceType, income.sourceId, wallets, cards, stashes),
                relativeDate = income.dateTime.toString().toRelativeDateString(now, zone),
            )
        }

        // ── Per-source list, sorted by last-use DESC (nulls = never used) ─
        // The user wants sources ordered so that "the last one used is
        // first" — most recently transacted (expense OR income) appears at
        // the top. Sources without any transaction fall to the end.
        // Subsequent .take(5) keeps only the 5 most recently used.
        val sortedPerSource = perSource
            .filter { it.balance.amount.signum() > 0 }
            .sortedWith(
                compareByDescending(nullsLast()) { 
                    lastUseBySource["${it.sourceType.name}:${it.sourceId}"]
                },
            )
            .take(5)

        return HomeUiState.Ready(
            totalBalance = total,
            perCurrencySubtotals = perCurrencySubtotals,
            perSource = sortedPerSource,
            recentExpenses = recentExpenses,
            recentIncomes = recentIncomes,
            rates = rates,
        )
    }

    private fun sourceLabelOf(
        sourceType: com.vida.domain.model.SourceType,
        sourceId: Long?,
        wallets: List<Wallet>,
        cards: List<Card>,
        stashes: List<Stash>,
    ): String = when (sourceType) {
        com.vida.domain.model.SourceType.WALLET -> {
            val wallet = sourceId?.let { id -> wallets.find { it.id == id } }
            wallet?.name ?: "Billetera"
        }
        com.vida.domain.model.SourceType.CARD -> {
            val card = sourceId?.let { id -> cards.find { it.id == id } }
            // Use the user-set note (acts as a display name) when present, else bank
            card?.note?.takeIf { it.isNotBlank() } ?: card?.bank ?: "Tarjeta"
        }
        com.vida.domain.model.SourceType.STASH -> {
            val stash = sourceId?.let { id -> stashes.find { it.id == id } }
            stash?.name ?: "Ahorro"
        }
    }
}