package com.t0h1.scanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun T0H1ScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = T0H1DarkColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
