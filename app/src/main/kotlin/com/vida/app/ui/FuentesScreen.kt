package com.vida.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vida.feature.cardmanagement.R as FeatR
import com.vida.domain.model.CardType
import com.vida.feature.cardmanagement.CardDisplayItem
import com.vida.feature.cardmanagement.CardFormDialog
import com.vida.feature.cardmanagement.CardListUiState
import com.vida.feature.cardmanagement.CardListViewModel
import com.vida.feature.cardmanagement.CardNavEvent
import com.vida.feature.transfermanagement.TransferFormDialog
import com.vida.feature.walletmanagement.WalletDisplayItem
import com.vida.feature.walletmanagement.WalletEditDialog
import com.vida.feature.walletmanagement.WalletListUiState
import com.vida.feature.walletmanagement.WalletNavEvent
import com.vida.feature.walletmanagement.WalletViewModel

// ── Card type badge helpers ──────────────────────────────────────────────────

private val DebitColor = Color(0xFF1565C0)
private val CreditColor = Color(0xFF2E7D32)
private val PrepaidColor = Color(0xFFE65100)

private val CardType.label: String
    get() = when (this) {
        CardType.DEBIT -> "DÉBITO"
        CardType.CREDIT -> "CRÉDITO"
        CardType.PREPAID -> "PREPAGO"
    }

private val CardType.badgeColor: Color
    get() = when (this) {
        CardType.DEBIT -> DebitColor
        CardType.CREDIT -> CreditColor
        CardType.PREPAID -> PrepaidColor
    }

// ── Bank brand identity ───────────────────────────────────────────────────────

private val BandecGradient: Brush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF8E0509).copy(alpha = 0.10f),
        Color(0xFF8E0509),
    ),
)

private val BPAColor: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFFBCD1DA) else Color(0xFF5C7882)
private val BPAGradient: Brush
    @Composable get() = Brush.horizontalGradient(
        colors = listOf(
            BPAColor.copy(alpha = 0.10f),
            BPAColor,
        ),
    )

private val MetropolitanoColor: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF91D506) else Color(0xFF3E6304)
private val MetropolitanoGradient: Brush
    @Composable get() = Brush.horizontalGradient(
        colors = listOf(
            MetropolitanoColor.copy(alpha = 0.10f),
            MetropolitanoColor,
        ),
    )

private data class BankBrand(
    val logoDrawable: Int?,
    val logoContentDescription: String,
    val gradient: Brush? = null,
    val logoTint: Color? = null,
)

@Composable
private fun bankBrandFor(bank: String): BankBrand = when (bank.trim().lowercase()) {
    "bandec" -> BankBrand(
        logoDrawable = FeatR.drawable.ic_bandec,
        logoContentDescription = "Bandec",
        gradient = BandecGradient,
        logoTint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    "bpa" -> BankBrand(
        logoDrawable = FeatR.drawable.ic_bpa,
        logoContentDescription = "BPA",
        gradient = BPAGradient,
    )
    "metropolitano" -> BankBrand(
        logoDrawable = FeatR.drawable.ic_metropolitano,
        logoContentDescription = "Metropolitano",
        gradient = MetropolitanoGradient,
    )
    else -> BankBrand(
        logoDrawable = null,
        logoContentDescription = "",
    )
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuentesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    onNavigateToCards: () -> Unit = {},
    walletViewModel: WalletViewModel = hiltViewModel(),
    cardListViewModel: CardListViewModel = hiltViewModel(),
) {
    val walletState by walletViewModel.uiState.collectAsState()
    val cardState by cardListViewModel.uiState.collectAsState()
    val isSavingWallet by walletViewModel.isSaving.collectAsState()
    val isSavingCard by cardListViewModel.isSaving.collectAsState()
    val bankNames by cardListViewModel.bankNames.collectAsStateWithLifecycle()
    val currencyCodes by cardListViewModel.currencyCodes.collectAsStateWithLifecycle()

    var showAddMenu by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }
    var showCardDialog by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<WalletDisplayItem?>(null) }
    var selectedCard by remember { mutableStateOf<CardDisplayItem?>(null) }
    var editingWallet by remember { mutableStateOf<WalletDisplayItem?>(null) }
    var editingCard by remember { mutableStateOf<CardDisplayItem?>(null) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Close dialogs on successful save
    LaunchedEffect(Unit) {
        walletViewModel.navEvents.collect { event ->
            if (event is WalletNavEvent.SaveSuccess) {
                showWalletDialog = false
                editingWallet = null
            }
        }
    }
    LaunchedEffect(Unit) {
        cardListViewModel.navEvents.collect { event ->
            if (event is CardNavEvent.SaveSuccess) {
                showCardDialog = false
                editingCard = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuentes") },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = { showTransferDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Transferir",
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar",
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // ── Wallet section header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Billeteras",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onNavigateToWallet) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Administrar billeteras",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = walletState) {
                is WalletListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is WalletListUiState.Ready -> {
                    state.wallets.forEach { wallet ->
                        WalletSummaryCard(
                            wallet = wallet,
                            onClick = { selectedWallet = wallet },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                is WalletListUiState.Empty -> {
                    Text(
                        text = "No hay billeteras registradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is WalletListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Cards section header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tarjetas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onNavigateToCards) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Administrar tarjetas",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = cardState) {
                is CardListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is CardListUiState.Ready -> {
                    if (state.cards.isEmpty()) {
                        Text(
                            text = "No hay tarjetas registradas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.cards.forEach { card ->
                            FullCardItem(
                                card = card,
                                onClick = { selectedCard = card },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                is CardListUiState.Empty -> {
                    Text(
                        text = "No hay tarjetas registradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is CardListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    // ── Add menu dialog ──────────────────────────────────────────────────────
    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            title = { Text("Agregar") },
            text = {
                Column {
                    TextButton(onClick = {
                        showAddMenu = false
                        showWalletDialog = true
                    }) {
                        Text("Nueva billetera")
                    }
                    TextButton(onClick = {
                        showAddMenu = false
                        showCardDialog = true
                    }) {
                        Text("Nueva tarjeta")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    // ── Wallet creation dialog ───────────────────────────────────────────────
    if (showWalletDialog) {
        WalletEditDialog(
            initialName = "",
            isEdit = false,
            isSaving = isSavingWallet,
            availableCurrencies = currencyCodes,
            onDismiss = { showWalletDialog = false },
            onSave = { name, currency, balanceMinor ->
                walletViewModel.onAdd(name, currency, balanceMinor)
            },
        )
    }

    // ── Card creation dialog ─────────────────────────────────────────────────
    if (showCardDialog) {
        CardFormDialog(
            isEdit = false,
            isSaving = isSavingCard,
            availableBanks = bankNames,
            availableCurrencies = currencyCodes,
            onDismiss = { showCardDialog = false },
            onSave = { bank, first6, last4, type, currency, expiry, note, balanceMinor ->
                cardListViewModel.onAdd(bank, first6, last4, type, currency, expiry, note, balanceMinor)
            },
        )
    }

    // ── Wallet detail dialog ─────────────────────────────────────────────────
    selectedWallet?.let { wallet ->
        AlertDialog(
            onDismissRequest = { selectedWallet = null },
            title = { Text(wallet.name) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = wallet.balanceFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = wallet.currencyCode,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedWallet = null
                    editingWallet = wallet
                }) {
                    Text("Editar")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWallet = null }) {
                    Text("Cerrar")
                }
            },
        )
    }

    // ── Card detail dialog ───────────────────────────────────────────────────
    selectedCard?.let { card ->
        AlertDialog(
            onDismissRequest = { selectedCard = null },
            title = { Text(card.bank) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = card.balanceFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    HorizontalDivider()
                    Text(
                        text = card.formattedNumber,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = card.type.badgeColor,
                    ) {
                        Text(
                            text = card.type.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Moneda: ${card.currency}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Vence: ${card.expiryFormatted}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    card.note?.let { note ->
                        HorizontalDivider()
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedCard = null
                    editingCard = card
                }) {
                    Text("Editar")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCard = null }) {
                    Text("Cerrar")
                }
            },
        )
    }

    // ── Wallet edit dialog ───────────────────────────────────────────────────
    editingWallet?.let { wallet ->
        val balanceInput = wallet.balance.amount
            .setScale(2, java.math.RoundingMode.HALF_EVEN)
            .toPlainString()
        WalletEditDialog(
            initialName = wallet.name,
            initialCurrency = wallet.currency,
            balance = balanceInput,
            isEdit = true,
            isSaving = isSavingWallet,
            availableCurrencies = currencyCodes,
            onDismiss = { editingWallet = null },
            onSave = { name, currency, balanceMinor ->
                walletViewModel.onEdit(wallet.id, name, currency, balanceMinor)
            },
        )
    }

    // ── Card edit dialog ─────────────────────────────────────────────────────
    editingCard?.let { card ->
        val balanceInput = card.balance.amount
            .setScale(2, java.math.RoundingMode.HALF_EVEN)
            .toPlainString()
        CardFormDialog(
            initialBank = card.bank,
            initialFirst6 = card.first6,
            initialLast4 = card.last4,
            initialType = card.type,
            initialCurrency = card.currency,
            initialExpiry = card.expiry,
            initialNote = card.note ?: "",
            balanceStr = balanceInput,
            isEdit = true,
            isSaving = isSavingCard,
            availableBanks = bankNames,
            availableCurrencies = currencyCodes,
            onDismiss = { editingCard = null },
            onSave = { bank, first6, last4, type, currency, expiry, note, balanceMinor ->
                cardListViewModel.onEdit(card.id, bank, first6, last4, type, currency, expiry, note, balanceMinor)
            },
        )
    }

    // ── Transfer modal ───────────────────────────────────────────────────────
    if (showTransferDialog) {
        TransferFormDialog(
            onDismiss = { showTransferDialog = false },
            // After a successful transfer, force the source lists to refresh.
            // The reactive observation in each ViewModel should already cover
            // this, but the explicit refresh is a safety net while the
            // reactivity chain is being validated.
            onSaved = {
                walletViewModel.refresh()
                cardListViewModel.refresh()
            },
        )
    }
}

// ── Wallet card ──────────────────────────────────────────────────────────────

@Composable
private fun WalletSummaryCard(wallet: WalletDisplayItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = wallet.balanceFormatted,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = wallet.currencyCode,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Full card item (wallet-like, balance-emphasized) ─────────────────────────

@Composable
private fun FullCardItem(card: CardDisplayItem, onClick: () -> Unit) {
    val brand = bankBrandFor(card.bank)
    val cardModifier = if (brand.gradient != null) {
        Modifier
            .fillMaxWidth()
            .background(brush = brand.gradient, shape = RoundedCornerShape(12.dp))
    } else {
        Modifier.fillMaxWidth()
    }
    val cardColors = if (brand.gradient != null) {
        CardDefaults.cardColors(containerColor = Color.Transparent)
    } else {
        CardDefaults.cardColors()
    }
    Card(
        onClick = onClick,
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = cardColors,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                // Top: card name (user-defined "Nombre de tarjeta"), or the
                // bank name as fallback when no name was provided. Mirrors the
                // wallet card layout where the name is the primary identifier.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.note ?: card.bank,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Bank name next to the masked card number.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.bank,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = card.formattedNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = card.balanceFormatted,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = card.type.badgeColor,
                    ) {
                        Text(
                            text = card.type.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                    Text(
                        text = card.currency,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = card.expiryFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            brand.logoDrawable?.let { drawable ->
                val tint = brand.logoTint
                Image(
                    painter = painterResource(drawable),
                    contentDescription = brand.logoContentDescription,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .size(36.dp),
                    colorFilter = tint?.let { ColorFilter.tint(it) },
                )
            }
        }
    }
}
