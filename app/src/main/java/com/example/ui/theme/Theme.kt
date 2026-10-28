package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuminaDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F25),
    primaryContainer = Color(0xFF003842),
    onPrimaryContainer = Color(0xFFA8F5FF),
    secondary = ElectricMagenta,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF4F0B2D),
    onSecondaryContainer = Color(0xFFFFD8E6),
    tertiary = RoyalViolet,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF2E1A69),
    onTertiaryContainer = Color(0xFFE5DEFF),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = DeepStudioSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedStudioCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = GlassSurfaceBorder
)

private val LuminaLightColorScheme = lightColorScheme(
    primary = Color(0xFF007B8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F8FF),
    onPrimaryContainer = Color(0xFF002025),
    secondary = Color(0xFFD81B72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E6),
    onSecondaryContainer = Color(0xFF3E001D),
    tertiary = Color(0xFF5E35B1),
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = TextPrimaryLight,
    surface = LightStudioSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightStudioCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD0D6EB)
)

@Composable
fun LuminaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LuminaDarkColorScheme else LuminaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
