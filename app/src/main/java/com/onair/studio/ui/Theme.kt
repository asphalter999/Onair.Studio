package com.onair.studio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StudioDarkBg = Color(0xFF11111B)
val StudioSurface = Color(0xFF181825)
val StudioCard = Color(0xFF1E1E2E)
val StudioBorder = Color(0xFF313244)
val StudioText = Color(0xFFCDD6F4)
val StudioMuted = Color(0xFFA6ADC8)

val LiveCrimson = Color(0xFFFF3366)
val RecordOrange = Color(0xFFFAB387)
val StudioCyan = Color(0xFF89B4FA)
val VuGreen = Color(0xFFA6E3A1)
val VuYellow = Color(0xFFF9E2AF)
val VuRed = Color(0xFFF38BA8)

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    secondary = LiveCrimson,
    tertiary = RecordOrange,
    background = StudioDarkBg,
    surface = StudioSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = StudioText,
    onSurface = StudioText
)

@Composable
fun OnAirStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
