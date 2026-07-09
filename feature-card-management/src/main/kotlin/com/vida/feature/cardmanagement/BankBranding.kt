package com.vida.feature.cardmanagement

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.vida.feature.cardmanagement.R

/**
 * Per-bank visual identity for [CardListItem] cards. Duplicated locally
 * (same pattern lives in app/.../FuentesScreen.kt) so each module stays
 * independent — adding a bank means updating both copies.
 *
 * @param logoDrawable R.drawable id of the bank logo, or null for none.
 * @param logoContentDescription Accessibility text for the logo.
 * @param gradient Horizontal gradient (alpha 0.10 → full color) for the card bg, or null.
 * @param logoTint Color applied via ColorFilter.tint to the logo. Null keeps the logo's
 *                 baked-in colors.
 */
internal data class BankBrand(
    val logoDrawable: Int?,
    val logoContentDescription: String,
    val gradient: Brush? = null,
    val logoTint: Color? = null,
)

/** Bandec: static dark crimson. Looks fine in both light and dark themes. */
internal val BandecGradient: Brush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF8E0509).copy(alpha = 0.10f),
        Color(0xFF8E0509),
    ),
)

/** BPA brand color: app-theme-aware (light BPA brand color or darker dark variant). */
internal val BPAColor: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFBCD1DA)
    } else {
        Color(0xFF5C7882)
    }
internal val BPAGradient: Brush
    @Composable get() = Brush.horizontalGradient(
        colors = listOf(
            BPAColor.copy(alpha = 0.10f),
            BPAColor,
        ),
    )

/** Metropolitano brand color: app-theme-aware (vibrant lime in light, mossy in dark). */
internal val MetropolitanoColor: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) {
        Color(0xFF91D506)
    } else {
        Color(0xFF3E6304)
    }
internal val MetropolitanoGradient: Brush
    @Composable get() = Brush.horizontalGradient(
        colors = listOf(
            MetropolitanoColor.copy(alpha = 0.10f),
            MetropolitanoColor,
        ),
    )

/**
 * Maps a free-text bank name (case-insensitive, trimmed) to its [BankBrand].
 *
 * `@Composable` so theme-aware fields (e.g., BPA's lighter/darker color) can
 * resolve inline against the active MaterialTheme.
 */
@Composable
internal fun bankBrandFor(bank: String): BankBrand = when (bank.trim().lowercase()) {
    "bandec" -> BankBrand(
        logoDrawable = R.drawable.ic_bandec,
        logoContentDescription = "Bandec",
        gradient = BandecGradient,
        logoTint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    "bpa" -> BankBrand(
        logoDrawable = R.drawable.ic_bpa,
        logoContentDescription = "BPA",
        gradient = BPAGradient,
    )
    "metropolitano" -> BankBrand(
        logoDrawable = R.drawable.ic_metropolitano,
        logoContentDescription = "Metropolitano",
        gradient = MetropolitanoGradient,
    )
    else -> BankBrand(
        logoDrawable = null,
        logoContentDescription = "",
    )
}
