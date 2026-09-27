package com.mazkiplay.trade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Bull,
    onPrimary = Color(0xFF04140A),
    primaryContainer = BullDim,
    onPrimaryContainer = NightText,
    secondary = Gold,
    onSecondary = Color(0xFF1A1400),
    tertiary = Aqua,
    onTertiary = Color(0xFF00201F),
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceAlt,
    onSurfaceVariant = NightTextDim,
    outline = NightOutline,
    error = Bear,
    onError = Color(0xFF2A0505)
)

private val LightScheme = lightColorScheme(
    primary = BullDim,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F5D8),
    onPrimaryContainer = Color(0xFF052413),
    secondary = Color(0xFF8A6A00),
    onSecondary = Color.White,
    tertiary = Color(0xFF0E7490),
    onTertiary = Color.White,
    background = DayBg,
    onBackground = DayText,
    surface = DaySurface,
    onSurface = DayText,
    surfaceVariant = DaySurfaceAlt,
    onSurfaceVariant = DayTextDim,
    outline = DayOutline,
    error = BearDim,
    onError = Color.White
)

/**
 * App theme.
 *
 * @param themeMode "dark", "light" or "system" as chosen on the Settings screen.
 */
@Composable
fun MazkiplayTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = MazkiplayTypography,
        content = content
    )
}

/** Semantic helpers so screens never hard-code bull/bear colours. */
object TradeColors {
    fun bull(): Color = Bull
    fun bear(): Color = Bear
    fun forDirection(isUp: Boolean): Color = if (isUp) Bull else Bear
    fun forBias(score: Double): Color = when {
        score > 0.18 -> Bull
        score < -0.18 -> Bear
        else -> Gold
    }
}
