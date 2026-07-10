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
 *
 * When [colorOverride] is provided it takes precedence over compile-time colors,
 * allowing the card screen to pass the bank's color from the repository.
 * When `null` (default), the original compile-time color is used for backward
 * compatibility with known banks, and an empty brand (no logo, no gradient)
 * is returned for unknown banks.
 *
 * @param bank The bank name to look up (case-insensitive, trimmed).
 * @param colorOverride Optional ARGB color to use for the gradient instead of
 *   the compile-time color. When non-null, the gradient is built from this
 *   single color regardless of the bank name. When null, compile-time colors
 *   are used for known banks and no gradient for unknown banks.
 */
@Composable
internal fun bankBrandFor(
    bank: String,
    colorOverride: Int? = null,
): BankBrand {
    val normalized = bank.trim().lowercase()
    val isKnownBank = normalized in setOf("bandec", "bpa", "metropolitano")

    val gradient: Brush? = if (colorOverride != null) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(colorOverride).copy(alpha = 0.10f),
                Color(colorOverride),
            ),
        )
    } else if (isKnownBank) {
        when (normalized) {
            "bandec" -> BandecGradient
            "bpa" -> BPAGradient
            "metropolitano" -> MetropolitanoGradient
            else -> null
        }
    } else {
        null
    }

    return when (normalized) {
        "bandec" -> BankBrand(
            logoDrawable = R.drawable.ic_bandec,
            logoContentDescription = "Bandec",
            gradient = gradient,
            logoTint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        "bpa" -> BankBrand(
            logoDrawable = R.drawable.ic_bpa,
            logoContentDescription = "BPA",
            gradient = gradient,
        )
        "metropolitano" -> BankBrand(
            logoDrawable = R.drawable.ic_metropolitano,
            logoContentDescription = "Metropolitano",
            gradient = gradient,
        )
        else -> BankBrand(
            logoDrawable = null,
            logoContentDescription = "",
            gradient = gradient,
        )
    }
}
