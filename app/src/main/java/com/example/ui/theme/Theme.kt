package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PandanGreen,
    onPrimary = Color.White,
    primaryContainer = PandanContainerLight,
    onPrimaryContainer = OnPandanContainerLight,
    secondary = TurmericGold,
    onSecondary = Color.White,
    secondaryContainer = TurmericContainerLight,
    onSecondaryContainer = OnTurmericContainerLight,
    tertiary = CinnabarClay,
    onTertiary = Color.White,
    tertiaryContainer = CinnabarContainerLight,
    onTertiaryContainer = OnCinnabarContainerLight,
    background = IvoryParchment,
    onBackground = InkCharcoal,
    surface = IvoryParchment,
    onSurface = InkCharcoal,
    surfaceVariant = WarmLinen,
    onSurfaceVariant = MutedHerbGray,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WarmLinen,
    surfaceContainer = SoftHerbSurface,
    surfaceContainerHigh = Color(0xFFE3DDD0),
    outline = OutlineStone,
    outlineVariant = OutlineSubtle,
    error = CinnabarClay,
    errorContainer = CinnabarContainerLight,
    onErrorContainer = OnCinnabarContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PandanGreenLight,
    onPrimary = Color(0xFF06291D),
    primaryContainer = PandanContainerDark,
    onPrimaryContainer = Color(0xFFC5F2DD),
    secondary = TurmericGoldLight,
    onSecondary = Color(0xFF3E2600),
    secondaryContainer = TurmericContainerDark,
    onSecondaryContainer = Color(0xFFFFDFAA),
    tertiary = CinnabarClayLight,
    onTertiary = Color(0xFF4F1308),
    tertiaryContainer = CinnabarContainerDark,
    onTertiaryContainer = Color(0xFFFFDAD3),
    background = DeepMossBackground,
    onBackground = WarmCreamText,
    surface = DeepMossSurface,
    onSurface = WarmCreamText,
    surfaceVariant = DeepMossContainer,
    onSurfaceVariant = MutedSageText,
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = DeepMossSurface,
    surfaceContainer = DeepMossContainer,
    surfaceContainerHigh = Color(0xFF2B3D36),
    outline = Color(0xFF83928A),
    outlineVariant = Color(0xFF35453E),
    error = CinnabarClayLight,
    errorContainer = CinnabarContainerDark,
    onErrorContainer = Color(0xFFFFDAD3)
)

val HerbalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ThaiHerbalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = HerbalShapes,
        content = content
    )
}
