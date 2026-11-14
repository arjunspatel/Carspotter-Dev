package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CarSpotterLightColorScheme = lightColorScheme(
    primary = InkPrimary,
    onPrimary = PureWhite,
    primaryContainer = MistSurface,
    onPrimaryContainer = InkPrimary,
    secondary = CobaltAccent,
    onSecondary = PureWhite,
    secondaryContainer = CobaltSoft,
    onSecondaryContainer = CobaltAccent,
    tertiary = RacingRed,
    onTertiary = PureWhite,
    tertiaryContainer = RacingRedSoft,
    onTertiaryContainer = RacingRed,
    background = PureWhite,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = PorcelainSurface,
    onSurfaceVariant = InkSecondary,
    outline = HairlineBorder,
    outlineVariant = MistSurface
)

@Composable
fun CarSpotterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CarSpotterLightColorScheme,
        typography = Typography,
        content = content
    )
}
