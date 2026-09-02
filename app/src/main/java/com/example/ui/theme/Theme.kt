package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = lightColorScheme(
    primary = NavyDark,
    onPrimary = Color.White,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = Color.White,
    secondary = SlateAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = TextPrimary,
    background = LightSurface,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorderSubtle,
    error = ReceivableRed,
    onError = Color.White
)

@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF0F172A),          // Crisp, dark text color
    unfocusedTextColor = Color(0xFF0F172A),        // Clearly visible dark text color
    disabledTextColor = Color(0xFF475569),
    errorTextColor = Color(0xFFDC2626),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color(0xFFF8FAFC),
    errorContainerColor = Color.White,
    cursorColor = Color(0xFF0F172A),               // Clearly visible dark cursor
    errorCursorColor = Color(0xFFDC2626),
    focusedBorderColor = Color(0xFF0F172A),        // Clean dark border when active
    unfocusedBorderColor = Color(0xFFCBD5E1),      // Subtle grey border
    errorBorderColor = Color(0xFFDC2626),
    focusedLabelColor = Color(0xFF0F172A),
    unfocusedLabelColor = Color(0xFF64748B),
    errorLabelColor = Color(0xFFDC2626),
    focusedPlaceholderColor = Color(0xFF94A3B8),   // Grey placeholder
    unfocusedPlaceholderColor = Color(0xFF94A3B8), // Grey placeholder
    focusedLeadingIconColor = Color(0xFF334155),
    unfocusedLeadingIconColor = Color(0xFF64748B),
    focusedTrailingIconColor = Color(0xFF334155),
    unfocusedTrailingIconColor = Color(0xFF64748B)
)

@Composable
fun MujahidAccountsTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MujahidAccountsTheme(darkTheme = darkTheme, content = content)
}
