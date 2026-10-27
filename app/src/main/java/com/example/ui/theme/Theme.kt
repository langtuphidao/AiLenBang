package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SunnyOrange,
    onPrimary = Color.White,
    primaryContainer = SoftPeachContainer,
    onPrimaryContainer = DeepClassroomNavy,
    secondary = OceanBlue,
    onSecondary = Color.White,
    secondaryContainer = SoftMintContainer,
    onSecondaryContainer = DeepClassroomNavy,
    tertiary = MintTeal,
    onTertiary = DeepClassroomNavy,
    background = WarmCreamBg,
    onBackground = DeepClassroomNavy,
    surface = WarmCardSurface,
    onSurface = DeepClassroomNavy,
    surfaceVariant = Color(0xFFF3EFE6),
    onSurfaceVariant = Color(0xFF3D5A6C),
    error = BerryPink,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8552),
    onPrimary = DeepClassroomNavy,
    primaryContainer = DarkContainer,
    onPrimaryContainer = Color(0xFFFFE5D9),
    secondary = Color(0xFF4CC9F0),
    onSecondary = DeepClassroomNavy,
    secondaryContainer = Color(0xFF1B4353),
    onSecondaryContainer = Color(0xFFD8F3DC),
    tertiary = MintTeal,
    onTertiary = DeepClassroomNavy,
    background = DarkBoardBg,
    onBackground = Color(0xFFF8F9FA),
    surface = DarkCardSurface,
    onSurface = Color(0xFFF8F9FA),
    surfaceVariant = Color(0xFF233D4D),
    onSurfaceVariant = Color(0xFFBFD7EA),
    error = Color(0xFFFF6B8B),
    onError = Color.White
)

val CartoonShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun AiLenBangTheme(
    darkTheme: Boolean = false,
    presentationMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val scale = if (presentationMode) 1.16f else 1.0f

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(scale),
        shapes = CartoonShapes,
        content = content
    )
}
