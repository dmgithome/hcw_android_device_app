package com.hv.cabinet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = PrimaryStart,
    onPrimary = BgDark,
    primaryContainer = PrimaryEnd,
    onPrimaryContainer = BgDark,
    secondary = AccentMint,
    onSecondary = BgDark,
    secondaryContainer = PanelBg,
    onSecondaryContainer = TextMain,
    tertiary = WarnStart,
    onTertiary = BgDark,
    error = DangerStart,
    onError = BgDark,
    background = BgDark,
    onBackground = TextMain,
    surface = PanelBg,
    onSurface = TextMain,
    surfaceVariant = BtnBaseStart,
    onSurfaceVariant = TextDim,
    outline = GlassBorder
)

@Composable
fun CabinetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = CabinetTypography,
        content = content
    )
}
