package com.lovealarm.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = BlushDeep,
    onPrimary = Cream,
    primaryContainer = Blush,
    onPrimaryContainer = Ink,
    secondary = LavenderDeep,
    onSecondary = Cream,
    secondaryContainer = Lavender,
    onSecondaryContainer = Ink,
    tertiary = MintDeep,
    onTertiary = Cream,
    tertiaryContainer = Mint,
    onTertiaryContainer = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Peach,
    onSurfaceVariant = InkSoft,
    outline = InkSoft,
    error = androidx.compose.ui.graphics.Color(0xFFB3261E),
    onError = Cream,
)

// Rounded everything. 24dp on tiles.
val LoveAlarmShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Stock typography for milestone 1. Nunito/Quicksand arrives in milestone 3.
val LoveAlarmTypography = Typography()

@Composable
fun LoveAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = LoveAlarmShapes,
        typography = LoveAlarmTypography,
        content = content,
    )
}
