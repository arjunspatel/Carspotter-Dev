package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CarSpotterColorScheme = darkColorScheme(
    primary = TurboAmber,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF331D00),
    onPrimaryContainer = TurboAmberGlow,
    secondary = NitroCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00363D),
    onSecondaryContainer = Color(0xFF80F3FF),
    tertiary = ApexCrimson,
    onTertiary = Color.White,
    background = CarbonDark,
    onBackground = TextPrimary,
    surface = CarbonCard,
    onSurface = TextPrimary,
    surfaceVariant = CarbonCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = CarbonBorder,
    outlineVariant = Color(0xFF1E2638)
)

@Composable
fun CarSpotterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CarSpotterColorScheme,
        typography = Typography,
        content = content
    )
}
