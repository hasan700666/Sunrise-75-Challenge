package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GoldenOrange,
    secondary = SunriseYellow,
    tertiary = LightYellow,
    background = DeepCharcoal,
    surface = DarkSurface,
    onPrimary = DeepCharcoal,
    onSecondary = DeepCharcoal,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for consistent brand styling
  dynamicColor: Boolean = false, // Disable dynamic colors to keep brand's Sunrise yellow/orange aesthetic
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
