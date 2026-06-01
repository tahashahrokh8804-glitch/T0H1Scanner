package com.t0h1.scanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val T0H1DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF06111F),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF06111F),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF06111F),
    background = Color(0xFF08111F),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF10192C),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF17233B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
)

private val T0H1Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun T0H1ScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = T0H1DarkColorScheme,
        typography = Typography(),
        shapes = T0H1Shapes,
        content = content,
    )
}
