package com.vida.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Thin wrapper around SharedPreferences for persisting [ThemeMode].
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("vida_prefs", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val ordinal = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)
        return ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.ordinal).apply()
    }

    private companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }
}

/**
 * Remember the persisted [ThemeMode] as reactive [MutableState].
 * Persists back to SharedPreferences on every change.
 */
@Composable
fun rememberThemeMode(): MutableState<ThemeMode> {
    val context = LocalContext.current
    val preferences = remember { ThemePreferences(context) }
    val mode = remember { mutableStateOf(preferences.getThemeMode()) }

    // Persist on every change (including the initial read, which is harmless)
    LaunchedEffect(mode.value) {
        preferences.setThemeMode(mode.value)
    }

    return mode
}
