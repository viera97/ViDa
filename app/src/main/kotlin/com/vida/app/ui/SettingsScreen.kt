package com.vida.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vida.app.BuildConfig
import com.vida.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToCurrencies: () -> Unit = {},
    onNavigateToBanks: () -> Unit = {},
    onNavigateToExportData: () -> Unit = {},
    onNavigateToImportData: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToTransfermovil: () -> Unit = {},
) {
    var showProDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Appearance section ────────────────────────────────────────
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column {
                    ThemeOption(
                        icon = Icons.Default.BrightnessMedium,
                        title = "Sistema",
                        description = "Usar el tema del dispositivo",
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    )
                    ThemeOption(
                        icon = Icons.Default.BrightnessHigh,
                        title = "Claro",
                        description = "Fondo claro siempre",
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    )
                    ThemeOption(
                        icon = Icons.Default.Brightness2,
                        title = "Oscuro",
                        description = "Fondo oscuro siempre",
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Options section ───────────────────────────────────────────
            Text(
                text = "Opciones",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            )

            SettingsOption(
                icon = Icons.Default.Category,
                title = "Categorías",
                description = "Administrar categorías de gastos e ingresos",
                onClick = onNavigateToCategories,
            )

            SettingsOption(
                icon = Icons.Default.CurrencyExchange,
                title = "Monedas",
                description = "Administrar monedas del sistema",
                onClick = onNavigateToCurrencies,
            )

            SettingsOption(
                icon = Icons.Default.AccountBalance,
                title = "Bancos",
                description = "Administrar bancos para tarjetas",
                onClick = onNavigateToBanks,
            )

            // ── Premium section ────────────────────────────────────────────
            Text(
                text = "Premium",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            )

            SettingsOption(
                icon = Icons.Default.FileUpload,
                title = "Exportar datos",
                description = "Descargar respaldo de tus finanzas",
                onClick = {
                    if (BuildConfig.IS_PREMIUM) {
                        onNavigateToExportData()
                    } else {
                        showProDialog = true
                    }
                },
            )

            SettingsOption(
                icon = Icons.Default.FileDownload,
                title = "Importar datos",
                description = "Restaurar respaldo de tus finanzas",
                onClick = {
                    if (BuildConfig.IS_PREMIUM) {
                        onNavigateToImportData()
                    } else {
                        showProDialog = true
                    }
                },
            )

            SettingsOption(
                icon = Icons.Default.Lock,
                title = "Seguridad",
                description = "PIN, huella y privacidad",
                onClick = {
                    if (BuildConfig.IS_PREMIUM) {
                        onNavigateToSecurity()
                    } else {
                        showProDialog = true
                    }
                },
            )

            SettingsOption(
                icon = Icons.Default.PhoneAndroid,
                title = "Transfermóvil",
                description = "Importación automática de gastos",
                onClick = {
                    if (BuildConfig.IS_PREMIUM) {
                        onNavigateToTransfermovil()
                    } else {
                        showProDialog = true
                    }
                },
            )

            // Future settings options go here
        }
    }

    // ── ViDa Pro modal ───────────────────────────────────────────────────
    if (showProDialog) {
        ViDaProDialog(onDismiss = { showProDialog = false })
    }

}

@Composable
private fun ThemeOption(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun SettingsOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ViDaProDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = "ViDa Pro",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Disfruta de funciones premium para llevar tus finanzas al siguiente nivel:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "• Reportes avanzados\n• Exportación de datos\n• Sincronización automática de tasas y gastos\n• Seguridad",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { uriHandler.openUri("https://vida.app/pro") }) {
                    Text("vida.app/pro")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}
