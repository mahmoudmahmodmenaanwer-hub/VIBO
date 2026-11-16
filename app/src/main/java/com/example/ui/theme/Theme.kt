package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.util.ThemeMode

private val ViboDarkColorScheme = darkColorScheme(
    primary = ViboCoral,
    onPrimary = Color.White,
    primaryContainer = ViboCoralContainerDark,
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = ViboCyan,
    onSecondary = Color(0xFF00201F),
    secondaryContainer = ViboCyanContainerDark,
    onSecondaryContainer = Color(0xFF9EF2F0),
    background = ObsidianBg,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianTextSecondary,
    outline = ObsidianOutline,
    error = ViboError,
    onError = Color.White
)

private val ViboLightColorScheme = lightColorScheme(
    primary = ViboCoral,
    onPrimary = Color.White,
    primaryContainer = ViboCoralContainerLight,
    onPrimaryContainer = Color(0xFF3E0014),
    secondary = Color(0xFF008B88),
    onSecondary = Color.White,
    secondaryContainer = ViboCyanContainerLight,
    onSecondaryContainer = Color(0xFF00201F),
    background = AlabasterBg,
    onBackground = AlabasterTextPrimary,
    surface = AlabasterSurface,
    onSurface = AlabasterTextPrimary,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = AlabasterTextSecondary,
    outline = AlabasterOutline,
    error = ViboError,
    onError = Color.White
)

val ViboShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ViboTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) ViboDarkColorScheme else ViboLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ViboTypography,
            shapes = ViboShapes,
            content = content
        )
    }
}
