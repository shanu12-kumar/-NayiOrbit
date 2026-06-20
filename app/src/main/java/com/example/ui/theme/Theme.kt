package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
      primary = PrimaryDark,
      onPrimary = OnPrimaryDark,
      primaryContainer = PrimaryContainerDark,
      onPrimaryContainer = OnPrimaryContainerDark,
      secondary = SecondaryDark,
      onSecondary = OnSecondaryDark,
      background = BackgroundDark,
      surface = SurfaceDark,
      onBackground = OnBackgroundDark,
      onSurface = OnSurfaceDark,
      outline = Color(0xFF424940),
      surfaceVariant = Color(0xFF2E322E),
      onSurfaceVariant = Color(0xFFC0C9BC),
      secondaryContainer = Color(0xFF4F250A),
      onSecondaryContainer = Color(0xFFF9DED0)
  )

private val LightColorScheme =
  lightColorScheme(
      primary = PrimaryLight,
      onPrimary = OnPrimaryLight,
      primaryContainer = PrimaryContainerLight,
      onPrimaryContainer = OnPrimaryContainerLight,
      secondary = SecondaryLight,
      onSecondary = OnSecondaryLight,
      background = BackgroundLight,
      surface = SurfaceLight,
      onBackground = OnBackgroundLight,
      onSurface = OnSurfaceLight,
      outline = BentoOutline,
      surfaceVariant = BentoSurfaceVariant,
      onSurfaceVariant = BentoOnSurfaceVariant,
      secondaryContainer = BentoSecondaryContainer,
      onSecondaryContainer = BentoOnSecondaryContainer
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
