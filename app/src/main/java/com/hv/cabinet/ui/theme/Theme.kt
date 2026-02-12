package com.hv.cabinet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Cyan400,
    onPrimary = Blue980,
    primaryContainer = Blue700,
    onPrimaryContainer = Gray100,
    secondary = Mint400,
    onSecondary = Blue980,
    secondaryContainer = Blue900,
    onSecondaryContainer = Gray100,
    tertiary = Amber400,
    onTertiary = Blue980,
    error = Red430,
    background = Blue980,
    onBackground = Gray100,
    surface = Blue950,
    onSurface = Gray100,
    surfaceVariant = Blue900,
    onSurfaceVariant = Gray300,
    outline = Gray300
)

@Composable
fun CabinetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = CabinetTypography,
        content = content
    )
}
