package com.profconq.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class PortPalette(
    val bg: Color,
    val bgElevated: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceInput: Color,
    val border: Color,
    val divider: Color,
    val text: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val heading: Color,
    val accent: Color,
    val accentHover: Color,
    val accentSoft: Color,
    val accentGlow: Color,
    val secondary: Color,
    val secondarySoft: Color,
    val play: Color,
    val danger: Color,
    val highlight: Color,
    val gold: Color,
    val info: Color,
    val warning: Color,
    val purple: Color,
    val brandNavy: Color,
    val brandBlue: Color,
    val brandCyan: Color,
    val brandGreen: Color,
    val brandYellow: Color,
    val gradientTop: Color,
    val gradientMid: Color,
    val gradientBottom: Color,
)

val LocalPortPalette = staticCompositionLocalOf { DarkPortPalette }

/** Primary CTA — darker forest green (outlined buttons use dark fill + this border). */
private val AccentTech = Color(0xFF2E9348)
private val AccentTechHover = Color(0xFF26803F)
private val AccentTechSoft = Color(0x262E9348)

val DarkPortPalette = PortPalette(
    bg = Color(0xFF060D1A),
    bgElevated = Color(0xFF0A1528),
    surface = Color(0xFF0F1D32),
    surfaceMuted = Color(0xFF0C1728),
    surfaceInput = Color(0xFF142440),
    border = Color(0x334A7BC4),
    divider = Color(0x14FFFFFF),
    text = Color(0xFFE2E8F4),
    textMuted = Color(0xFF8BA3C7),
    textSubtle = Color(0xFF5C7294),
    heading = Color(0xFFFFFFFF),
    accent = AccentTech,
    accentHover = AccentTechHover,
    accentSoft = AccentTechSoft,
    accentGlow = Color(0x3338C8F0),
    secondary = Color(0xFF38C8F0),
    secondarySoft = Color(0x2638C8F0),
    play = Color(0xFFF4C430),
    danger = Color(0xFFEF4444),
    highlight = Color(0xFFE8A838),
    gold = Color(0xFFF4C430),
    info = Color(0xFF38C8F0),
    warning = Color(0xFFEAB308),
    purple = Color(0xFF7C6CF0),
    brandNavy = Color(0xFF0A1E45),
    brandBlue = Color(0xFF1A4FA3),
    brandCyan = Color(0xFF38C8F0),
    brandGreen = AccentTech,
    brandYellow = Color(0xFFF4C430),
    gradientTop = Color(0xFF0A1E45).copy(alpha = 0.85f),
    gradientMid = Color(0xFF1A4FA3).copy(alpha = 0.35f),
    gradientBottom = Color(0xFF060D1A),
)

val LightPortPalette = PortPalette(
    bg = Color(0xFFF2F6FC),
    bgElevated = Color(0xFFE8EFF9),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF0F4FA),
    surfaceInput = Color(0xFFE2EAF5),
    border = Color(0x664A7BC4),
    divider = Color(0x1A0A1E45),
    text = Color(0xFF1A2B44),
    textMuted = Color(0xFF5C7294),
    textSubtle = Color(0xFF8BA3C7),
    heading = Color(0xFF0A1E45),
    accent = Color(0xFF2A7D42),
    accentHover = Color(0xFF236B38),
    accentSoft = Color(0x332A7D42),
    accentGlow = Color(0x3338C8F0),
    secondary = Color(0xFF1A8FB8),
    secondarySoft = Color(0x2638C8F0),
    play = Color(0xFFD4A820),
    danger = Color(0xFFDC2626),
    highlight = Color(0xFFD4941A),
    gold = Color(0xFFD4A820),
    info = Color(0xFF1A8FB8),
    warning = Color(0xFFCA8A04),
    purple = Color(0xFF6D5CE8),
    brandNavy = Color(0xFF0A1E45),
    brandBlue = Color(0xFF1A4FA3),
    brandCyan = Color(0xFF38C8F0),
    brandGreen = Color(0xFF2A7D42),
    brandYellow = Color(0xFFF4C430),
    gradientTop = Color(0xFF38C8F0).copy(alpha = 0.12f),
    gradientMid = Color(0xFF1A4FA3).copy(alpha = 0.08f),
    gradientBottom = Color(0xFFF2F6FC),
)

fun PortPalette.toColorScheme(): ColorScheme {
    val dark = bg.luminance() < 0.5f
    return if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = brandNavy,
            primaryContainer = accentSoft,
            secondary = secondary,
            onSecondary = brandNavy,
            tertiary = play,
            background = bg,
            surface = surface,
            surfaceVariant = surfaceMuted,
            onBackground = text,
            onSurface = text,
            onSurfaceVariant = textMuted,
            outline = border,
            outlineVariant = divider,
            error = danger,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accentSoft,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = play,
            background = bg,
            surface = surface,
            surfaceVariant = surfaceMuted,
            onBackground = text,
            onSurface = text,
            onSurfaceVariant = textMuted,
            outline = border,
            outlineVariant = divider,
            error = danger,
        )
    }
}

fun PortPalette.toTypography(): Typography {
    val headingColor = heading
    val bodyColor = text
    val mutedColor = textMuted
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            color = headingColor,
            letterSpacing = (-0.2).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            color = headingColor,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = headingColor,
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = bodyColor,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = bodyColor,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = mutedColor,
            letterSpacing = 0.2.sp,
        ),
    )
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
