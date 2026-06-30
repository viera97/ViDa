package com.vida.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val FallbackLightColorScheme = lightColorScheme(
    primary = FallbackLightPrimary,
    onPrimary = FallbackLightOnPrimary,
    primaryContainer = FallbackLightPrimaryContainer,
    onPrimaryContainer = FallbackLightOnPrimaryContainer,
    secondary = FallbackLightSecondary,
    onSecondary = FallbackLightOnSecondary,
    secondaryContainer = FallbackLightSecondaryContainer,
    onSecondaryContainer = FallbackLightOnSecondaryContainer,
    tertiary = FallbackLightTertiary,
    onTertiary = FallbackLightOnTertiary,
    tertiaryContainer = FallbackLightTertiaryContainer,
    onTertiaryContainer = FallbackLightOnTertiaryContainer,
    error = FallbackLightError,
    onError = FallbackLightOnError,
    errorContainer = FallbackLightErrorContainer,
    onErrorContainer = FallbackLightOnErrorContainer,
    background = FallbackLightBackground,
    onBackground = FallbackLightOnBackground,
    surface = FallbackLightSurface,
    onSurface = FallbackLightOnSurface,
    surfaceVariant = FallbackLightSurfaceVariant,
    onSurfaceVariant = FallbackLightOnSurfaceVariant,
    outline = FallbackLightOutline,
)

private val FallbackDarkColorScheme = darkColorScheme(
    primary = FallbackDarkPrimary,
    onPrimary = FallbackDarkOnPrimary,
    primaryContainer = FallbackDarkPrimaryContainer,
    onPrimaryContainer = FallbackDarkOnPrimaryContainer,
    secondary = FallbackDarkSecondary,
    onSecondary = FallbackDarkOnSecondary,
    secondaryContainer = FallbackDarkSecondaryContainer,
    onSecondaryContainer = FallbackDarkOnSecondaryContainer,
    tertiary = FallbackDarkTertiary,
    onTertiary = FallbackDarkOnTertiary,
    tertiaryContainer = FallbackDarkTertiaryContainer,
    onTertiaryContainer = FallbackDarkOnTertiaryContainer,
    error = FallbackDarkError,
    onError = FallbackDarkOnError,
    errorContainer = FallbackDarkErrorContainer,
    onErrorContainer = FallbackDarkOnErrorContainer,
    background = FallbackDarkBackground,
    onBackground = FallbackDarkOnBackground,
    surface = FallbackDarkSurface,
    onSurface = FallbackDarkOnSurface,
    surfaceVariant = FallbackDarkSurfaceVariant,
    onSurfaceVariant = FallbackDarkOnSurfaceVariant,
    outline = FallbackDarkOutline,
)

@Composable
fun ViDaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDarkColorScheme
        else -> FallbackLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ViDaTypography,
        content = content
    )
}
