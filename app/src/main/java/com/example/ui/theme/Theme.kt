package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LunaLightColorScheme = lightColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = RoseSoftContainer,
    onPrimaryContainer = RoseOnSoftContainer,
    secondary = PinkSecondary,
    onSecondary = Color.White,
    secondaryContainer = PinkSoftContainer,
    onSecondaryContainer = PinkOnSoftContainer,
    tertiary = SkyWater,
    onTertiary = Color.White,
    tertiaryContainer = SkyWaterLight,
    onTertiaryContainer = SkyWaterDarkText,
    background = LightBgTop,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFFFF0F4),
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFFECDD3),
    outlineVariant = Color(0xFFFCE7F3)
)

private val LunaDarkColorScheme = darkColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C0D22),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = PinkSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1032),
    onSecondaryContainer = Color(0xFFFBCFE8),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = DarkBgTop,
    onBackground = DarkTextPrimary,
    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF261E2A),
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1FFFFFFF)
)

val LunaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LunaCareTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LunaDarkColorScheme else LunaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LunaCareTypography,
        shapes = LunaShapes,
        content = content
    )
}
