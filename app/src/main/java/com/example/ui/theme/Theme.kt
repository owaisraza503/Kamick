package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5252), // Energetic manga red/coral
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C0000),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF00E5FF), // Cyan accent
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D59),
    onSecondaryContainer = Color(0xFF70F5FF),
    tertiary = Color(0xFFFFB74D), // Golden amber
    background = Color(0xFF121216),
    onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF1A1A22),
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF252530),
    onSurfaceVariant = Color(0xFFC8C5D0)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5252),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0000),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00363F),
    onSecondaryContainer = Color(0xFF70F5FF),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF0EFF4),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF0EFF4),
    surfaceVariant = Color(0xFF141418),
    onSurfaceVariant = Color(0xFFCAC5D0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4F1FA),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFFF57C00),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF0EDF6),
    onSurfaceVariant = Color(0xFF46454F)
)

@Composable
fun KamickTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        "AMOLED" -> true
        else -> systemDark
    }

    val colorScheme = when {
        themeMode == "AMOLED" -> AmoledDarkColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    KamickTheme(
        themeMode = if (darkTheme) "DARK" else "LIGHT",
        content = content
    )
}

