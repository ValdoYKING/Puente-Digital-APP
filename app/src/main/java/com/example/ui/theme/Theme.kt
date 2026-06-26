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
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekDarkPrimary,
    secondary = SleekDarkSecondary,
    tertiary = SleekDarkTertiary,
    background = SleekDarkBackground,
    surface = SleekDarkSurface,
    surfaceVariant = SleekDarkSurfaceVariant,
    onPrimary = SleekDarkOnPrimary,
    onSecondary = SleekDarkOnSecondary,
    onBackground = SleekDarkOnBackground,
    onSurface = SleekDarkOnSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    secondary = SleekSecondary,
    tertiary = SleekTertiary,
    background = SleekBackground,
    surface = SleekSurface,
    surfaceVariant = SleekSurfaceVariant,
    outline = SleekOutline,
    onPrimary = SleekOnPrimary,
    onSecondary = SleekOnSecondary,
    onBackground = SleekOnBackground,
    onSurface = SleekOnSurface,
    onSurfaceVariant = SleekOnSurfaceVariant
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
