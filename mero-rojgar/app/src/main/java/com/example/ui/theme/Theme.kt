package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LightColorScheme = lightColorScheme(
    primary = HimalayanNavy,
    onPrimary = Color.White,
    primaryContainer = SapphireContainer,
    onPrimaryContainer = OnSapphireContainer,
    secondary = NepalCrimson,
    onSecondary = Color.White,
    secondaryContainer = CrimsonContainer,
    onSecondaryContainer = OnCrimsonContainer,
    tertiary = SaffronGold,
    onTertiary = Color.White,
    tertiaryContainer = SaffronSoft,
    onTertiaryContainer = OnSaffronContainer,
    background = SurfaceWarm,
    onBackground = TextPrimaryLight,
    surface = SurfaceCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = DangerRed,
    errorContainer = DangerContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8BB8FF),
    onPrimary = Color(0xFF062145),
    primaryContainer = SapphireBlue,
    onPrimaryContainer = Color(0xFFD9E6F7),
    secondary = Color(0xFFFF8990),
    onSecondary = Color(0xFF560010),
    secondaryContainer = Color(0xFF8C1424),
    onSecondaryContainer = CrimsonContainer,
    tertiary = Color(0xFFFFC966),
    onTertiary = Color(0xFF3E2600),
    tertiaryContainer = Color(0xFF5D3A00),
    onTertiaryContainer = SaffronSoft,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

val MeroRojgarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MeroRojgarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isNepali: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(isNepali = isNepali),
        shapes = MeroRojgarShapes,
        content = content
    )
}
