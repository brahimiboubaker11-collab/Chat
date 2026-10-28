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

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Navy900,
    primaryContainer = CyanDark,
    onPrimaryContainer = Color.White,
    secondary = AmberAccent,
    onSecondary = Navy900,
    secondaryContainer = AmberGlow,
    onSecondaryContainer = Color.White,
    tertiary = EmeraldSuccess,
    background = Navy900,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkCard,
    onSurfaceVariant = TextSecondaryDark,
    error = RoseSilent,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = AmberGlow,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFFB45309),
    tertiary = EmeraldSuccess,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLightCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    error = RoseSilent,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek tech dark theme which matches the audio utility vibe
    dynamicColor: Boolean = false, // Keep consistent branding colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
