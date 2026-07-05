package com.smeet.telesis.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object VaultColors {
    val Ink = Color(0xFF090A0D)
    val Panel = Color(0xFF11131A)
    val PanelAlt = Color(0xFF171A22)
    val Gold = Color(0xFFD8B46A)
    val GoldSoft = Color(0xFFFFE1A1)
    val Emerald = Color(0xFF39D98A)
    val Danger = Color(0xFFFF6B6B)
    val Blue = Color(0xFF7AA2FF)
    val Muted = Color(0xFF9AA3B2)
    val Border = Color(0x22FFFFFF)
    val Glass = Color(0x14FFFFFF)

    val HeroBrush = Brush.linearGradient(listOf(Color(0xFF1C2030), Color(0xFF11131A), Color(0xFF0B0C10)))
}

private val DarkScheme = darkColorScheme(
    primary = VaultColors.Gold,
    onPrimary = Color(0xFF15120A),
    secondary = VaultColors.Emerald,
    background = VaultColors.Ink,
    surface = VaultColors.Panel,
    surfaceVariant = VaultColors.PanelAlt,
    onBackground = Color(0xFFF6F2EA),
    onSurface = Color(0xFFF6F2EA),
    onSurfaceVariant = VaultColors.Muted,
    error = VaultColors.Danger
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF8A641D),
    onPrimary = Color.White,
    secondary = Color(0xFF0B7F4A),
    background = Color(0xFFF7F2EA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1E8DA),
    onBackground = Color(0xFF141414),
    onSurface = Color(0xFF141414),
    onSurfaceVariant = Color(0xFF5F6368),
    error = Color(0xFFB3261E)
)

@Composable
fun TelesisTheme(content: @Composable () -> Unit) {
    val scheme: ColorScheme = if (isSystemInDarkTheme()) DarkScheme else DarkScheme
    MaterialTheme(colorScheme = scheme, typography = androidx.compose.material3.Typography(), content = content)
}
