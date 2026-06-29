package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CosmicTeal,
    secondary = CosmicBlue,
    tertiary = CosmicPurple,
    background = SpaceBlack,
    surface = SlateDark,
    onPrimary = SpaceBlack,
    onSecondary = SpaceBlack,
    onBackground = OffWhite,
    onSurface = OffWhite
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicBlue,
    secondary = CosmicPurple,
    tertiary = CosmicTeal,
    background = OffWhite,
    surface = SlateDark,
    onPrimary = OffWhite,
    onSecondary = OffWhite,
    onBackground = SpaceBlack,
    onSurface = OffWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme by default for the sleek Cosmic Slate vibe
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
