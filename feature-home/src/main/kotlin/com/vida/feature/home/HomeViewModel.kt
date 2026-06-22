package com.vida.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Card
import com.vida.domain.model.Currency
import com.vida.domain.model.Money
import com.vida.domain.model.Stash
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.usecase.balance.GetTotalBalance
import com.vida.domain.usecase.card.GetCardBalance
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.expense.ListExpenses
import com.vida.domain.usecase.rate.GetCurrentRate
import com.vida.domain.usecase.stash.GetStashBalance
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWalletBalance
import com.vida.feature.home.util.formatMoney
import com.vida.feature.home.util.toRelativeDateString
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
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val getWalletBalance: GetWalletBalance,
    private val getCardBalance: GetCardBalance,
    private val getStashBalance: GetStashBalance,
    private val getCurrentRate: GetCurrentRate,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Trigger flow: any change to expenses, cards, or stashes causes a
     * re-derivation of the full dashboard state.
     */
    private val triggerFlow: Flow<Unit> = combine(
        listExpenses(),
        listCards(),
        listStashes(),
    ) { _, _, _ -> Unit }

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
     */
    private suspend fun computeState(): HomeUiState {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()

        // ── Per-source balances (fail-fast per S2) ─────────────────────────
        val walletBalance = try {
            getWalletBalance(asOf = now)
        } catch (_: Exception) {
            return HomeUiState.Error("Error de balance")
        }

        val cards: List<Card> = listCards().first()
        val cardBalances = cards.map { card ->
            val balance = try {
                getCardBalance(card.id, now)
            } catch (_: Exception) {
                return HomeUiState.Error("Error de tarjeta")
            }
            card to balance
        }

        val stashes: List<Stash> = listStashes().first()
        val stashBalances = stashes.map { stash ->
            val balance = try {
                getStashBalance(stash.id, now)
            } catch (_: Exception) {
                return HomeUiState.Error("Error de ahorro")
            }
            stash to balance
        }

        // ── Per-source list ───────────────────────────────────────────────
        val perSource = mutableListOf<PerSource>()
        perSource.add(
            PerSource(
                label = "Billetera",
                balance = walletBalance,
                formatted = formatMoney(walletBalance),
            ),
        )
        for ((card, balance) in cardBalances) {
            perSource.add(
                PerSource(
                    label = card.bank,
                    balance = balance,
                    formatted = formatMoney(balance),
                ),
            )
        }
        for ((stash, balance) in stashBalances) {
            perSource.add(
                PerSource(
                    label = stash.name,
                    balance = balance,
                    formatted = formatMoney(balance),
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
        val expenses = listExpenses().first().take(5)

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
                formattedAmount = formatMoney(expense.amount),
                sourceLabel = sourceLabelOf(expense.sourceType, expense.sourceId, cards, stashes),
                relativeDate = expense.dateTime.toString().toRelativeDateString(now, zone),
            )
        }

        return HomeUiState.Ready(
            totalBalance = total,
            perCurrencySubtotals = perCurrencySubtotals,
            perSource = perSource.filter { it.balance.amount.signum() > 0 },
            recentExpenses = recentExpenses,
            rates = rates,
        )
    }

    private fun sourceLabelOf(
        sourceType: com.vida.domain.model.SourceType,
        sourceId: Long?,
        cards: List<Card>,
        stashes: List<Stash>,
    ): String = when (sourceType) {
        com.vida.domain.model.SourceType.WALLET -> "Billetera"
        com.vida.domain.model.SourceType.CARD -> {
            val card = cards.find { it.id == sourceId }
            card?.bank ?: "Tarjeta"
        }
        com.vida.domain.model.SourceType.STASH -> {
            val stash = stashes.find { it.id == sourceId }
            stash?.name ?: "Ahorro"
        }
    }
}