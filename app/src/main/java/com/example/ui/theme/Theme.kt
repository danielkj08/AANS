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
    primary = PolishPrimaryDark,
    onPrimary = PolishOnPrimaryDark,
    primaryContainer = PolishPrimaryContainerDark,
    onPrimaryContainer = PolishOnPrimaryContainerDark,
    secondary = PolishSecondaryDark,
    onSecondary = PolishOnSecondaryDark,
    secondaryContainer = PolishSecondaryContainerDark,
    onSecondaryContainer = PolishOnSecondaryContainerDark,
    tertiary = PolishTertiaryDark,
    background = PolishBackgroundDark,
    onBackground = PolishOnBackgroundDark,
    surface = PolishSurfaceDark,
    onSurface = PolishOnSurfaceDark,
    surfaceVariant = PolishSurfaceVariantDark,
    onSurfaceVariant = PolishOnSurfaceVariantDark,
    outline = PolishOutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PolishPrimaryLight,
    onPrimary = PolishOnPrimaryLight,
    primaryContainer = PolishPrimaryContainerLight,
    onPrimaryContainer = PolishOnPrimaryContainerLight,
    secondary = PolishSecondaryLight,
    onSecondary = PolishOnSecondaryLight,
    secondaryContainer = PolishSecondaryContainerLight,
    onSecondaryContainer = PolishOnSecondaryContainerLight,
    tertiary = PolishTertiaryLight,
    background = PolishBackgroundLight,
    onBackground = PolishOnBackgroundLight,
    surface = PolishSurfaceLight,
    onSurface = PolishOnSurfaceLight,
    surfaceVariant = PolishSurfaceVariantLight,
    onSurfaceVariant = PolishOnSurfaceVariantLight,
    outline = PolishOutlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // We disable dynamic color to strictly project the Professional Polish curated aesthetic
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
