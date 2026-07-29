package com.vida.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vida.domain.model.Card
import com.vida.domain.model.Currency
import com.vida.domain.model.Expense
import com.vida.domain.model.Income
import com.vida.domain.model.Money
import com.vida.domain.model.SourceType
import com.vida.domain.model.Stash
import com.vida.domain.model.Wallet
import kotlin.comparisons.nullsLast
import com.vida.domain.repository.CategoryRepository
import com.vida.domain.repository.ExpenseRepository
import com.vida.domain.repository.IncomeRepository
import com.vida.domain.usecase.ConvertCurrency
import com.vida.domain.usecase.card.GetCardBalance
import com.vida.domain.usecase.card.ListCards
import com.vida.domain.usecase.rate.GetCurrentRate
import com.vida.domain.usecase.rate.ListCurrencyRates
import com.vida.domain.usecase.stash.GetStashBalance
import com.vida.domain.usecase.stash.ListStashes
import com.vida.domain.usecase.wallet.GetWalletBalance
import com.vida.domain.usecase.wallet.ListWallets
import com.vida.core.format.toRelativeDateString
import com.vida.feature.home.cache.DashboardCache
import com.vida.feature.home.home.formatHomeMoney
import com.vida.feature.home.update.ApkInstaller
import com.vida.feature.home.update.ReleaseAsset
import com.vida.feature.home.update.UpdateCheckResult
import com.vida.feature.home.update.UpdateManager
import com.vida.feature.home.update.UpdateUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
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
    private val dashboardCache: DashboardCache,
    private val convertCurrency: ConvertCurrency,
    private val listCards: ListCards,
    private val listStashes: ListStashes,
    private val getWalletBalance: GetWalletBalance,
    private val getCardBalance: GetCardBalance,
    private val getStashBalance: GetStashBalance,
    private val listWallets: ListWallets,
    private val getCurrentRate: GetCurrentRate,
    private val listCurrencyRates: ListCurrencyRates,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val updateManager: UpdateManager,
    private val apkInstaller: ApkInstaller,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    /**
     * Holds the asset + htmlUrl from the latest `UpdateAvailable` so the
     * "Descargar" button can re-enter the download flow without a new check.
     */
    private var pendingUpdate: PendingUpdate? = null

    /**
     * Trigger flow: any change to expenses, incomes, cards, or stashes causes a
     * re-derivation of the full dashboard state.
     *
     * Uses `observeRecent(1)` for expenses/incomes — a single-row query — so Room
     * still detects table invalidations but without a full-table scan + SQLCipher
     * decryption of every row just to know that *something* changed.
     *
     * Expenses + incomes are nested in a single combine first so the outer
     * combine stays at the 5-arg overload ceiling — adding a 6th peer
     * (incomes) without nesting would require the vararg overload and lose
     * type inference.
     */
    private val triggerFlow: Flow<Unit> = combine(
        combine(
            expenseRepository.observeRecent(1),
            incomeRepository.observeRecent(1),
        ) { _, _ -> Unit },
        listCards(),
        listStashes(),
        listWallets(),
        listCurrencyRates(),
    ) { _, _, _, _, _ -> Unit }

    init {
        viewModelScope.launch {
            // Stale-while-revalidate: show cached data instantly, then refresh
            val cached = dashboardCache.load()
            _uiState.value = cached ?: HomeUiState.Loading

            triggerFlow.collect {
                val state = computeState()
                if (state is HomeUiState.Ready) {
                    dashboardCache.save(state)
                }
                _uiState.value = state
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

        // ── Phase 1: load entity lists in parallel ─────────────────────────
        val wallets: List<Wallet>
        val cards: List<Card>
        val stashes: List<Stash>
        val expenses: List<Expense>
        val incomes: List<Income>
        coroutineScope {
            val wDef = async { listWallets().first() }
            val cDef = async { listCards().first() }
            val sDef = async { listStashes().first() }
            val eDef = async { expenseRepository.observeRecent(5).first() }
            val iDef = async { incomeRepository.observeRecent(5).first() }
            wallets = wDef.await()
            cards = cDef.await()
            stashes = sDef.await()
            expenses = eDef.await()
            incomes = iDef.await()
        }

        // ── Per-source balances (fail-fast per S2) ─────────────────────────
        val walletBalances = wallets.map { wallet ->
            val balance = try {
                getWalletBalance(wallet.id).first()
            } catch (_: Exception) {
                return HomeUiState.Error("Error de balance")
            }
            wallet to balance
        }

        val cardBalances = cards.map { card ->
            val balance = try {
                getCardBalance(card.id).first()
            } catch (_: Exception) {
                return HomeUiState.Error("Error de tarjeta")
            }
            card to balance
        }

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

        // ── Total balance (computed from per-source data, no extra queries) ──
        val total = try {
            perSource.fold(Money.ZERO_CUP) { acc, source ->
                val inCup = convertCurrency(source.balance, Currency.CUP, now) ?: Money.ZERO_CUP
                acc + inCup
            }
        } catch (t: Throwable) {
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            return HomeUiState.Error(t.message ?: "Error de balance")
        }

        // ── Per-source last-use map (for "most recently used" ordering) ───
        // Key = "${sourceType}:${sourceId}". Value = max date across expenses
        // and incomes for that source. Built from the last 5 of each, which
        // covers the most recently used sources. Sources without transactions
        // in those recent items fall to the end of the sort.
        val lastUseBySource: Map<String, Instant> = buildMap {
            for (e in expenses) {
                val key = "${e.sourceType.name}:${e.sourceId}"
                val existing = this[key]
                if (existing == null || e.dateTime > existing) {
                    this[key] = e.dateTime
                }
            }
            for (i in incomes) {
                val key = "${i.sourceType.name}:${i.sourceId}"
                val existing = this[key]
                if (existing == null || i.dateTime > existing) {
                    this[key] = i.dateTime
                }
            }
        }

        // ── Rates (graceful degradation per S3) ────────────────────────────
        val rates: Map<String, java.math.BigDecimal>? = try {
            val usdRate = getCurrentRate("USD", "CUP", now)
            val mlcRate = getCurrentRate("MLC", "CUP", now)
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

    // ── In-app updater ────────────────────────────────────────────────────
    //
    // The flow is driven by the user tapping the SystemUpdate IconButton in
    // the Home TopAppBar. The state machine lives in [UpdateUiState]; the
    // dialogs (UpdateAvailable / Downloading / ReadyToInstall) are rendered
    // by HomeScreen based on [updateState], and the transient UpToDate /
    // Error states are surfaced via a snackbar in the Scaffold.

    /**
     * Starts a release check. Transitions [updateState] through
     * `Checking → UpToDate | UpdateAvailable | Error`.
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Checking
            _updateState.value = try {
                when (val result = updateManager.check()) {
                    is UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate(result.currentVersion)
                    is UpdateCheckResult.Available -> {
                        if (result.asset == null) {
                            UpdateUiState.Error("La versión $result.version no tiene APK descargable")
                        } else {
                            pendingUpdate = PendingUpdate(asset = result.asset, htmlUrl = result.htmlUrl)
                            UpdateUiState.UpdateAvailable(
                                version = result.version,
                                sizeBytes = result.asset.sizeBytes,
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is kotlin.coroutines.cancellation.CancellationException) throw t
                UpdateUiState.Error(t.message ?: "Error al buscar actualizaciones")
            }
        }
    }

    /**
     * Downloads the APK for the most recent [UpdateUiState.UpdateAvailable].
     * No-op if there is no pending update (defensive — should not happen via
     * the UI).
     */
    fun startDownload(context: Context) {
        val pending = pendingUpdate ?: return
        viewModelScope.launch {
            _updateState.value = UpdateUiState.Downloading(progress = 0f)
            _updateState.value = try {
                val file = updateManager.download(
                    asset = pending.asset,
                    destFile = destFile(context),
                ) { progress ->
                    // emitted on IO; hop back to the main thread is not needed
                    // because StateFlow.value is thread-safe.
                    _updateState.value = UpdateUiState.Downloading(progress.fraction)
                }
                UpdateUiState.ReadyToInstall(file)
            } catch (t: Throwable) {
                if (t is kotlin.coroutines.cancellation.CancellationException) throw t
                UpdateUiState.Error(t.message ?: "Error al descargar la actualización")
            }
        }
    }

    /**
     * Hands the downloaded file to the system installer. Called from the
     * "Instalar" button in the [UpdateUiState.ReadyToInstall] dialog.
     */
    fun installUpdate(file: File) {
        viewModelScope.launch {
            try {
                apkInstaller.install(file)
                _updateState.value = UpdateUiState.Idle
            } catch (t: Throwable) {
                if (t is kotlin.coroutines.cancellation.CancellationException) throw t
                _updateState.value = UpdateUiState.Error(t.message ?: "Error al abrir el instalador")
            }
        }
    }

    /**
     * Resets the update state to [UpdateUiState.Idle]. Called by the UI
     * after a transient snackbar (UpToDate / Error) dismisses, and when the
     * user explicitly cancels the update dialog.
     */
    fun dismissUpdateDialog() {
        pendingUpdate = null
        _updateState.value = UpdateUiState.Idle
    }

    private fun destFile(context: Context): File {
        val dir = File(context.cacheDir, "updates")
        dir.mkdirs()
        return File(dir, "app-release.apk")
    }

    private data class PendingUpdate(
        val asset: ReleaseAsset,
        val htmlUrl: String,
    )
}