package com.profconq.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val palette: PortPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalPortPalette.current

val PpBrandNavy: Color
    @Composable @ReadOnlyComposable get() = palette.brandNavy
val PpBrandBlue: Color
    @Composable @ReadOnlyComposable get() = palette.brandBlue
val PpBrandCyan: Color
    @Composable @ReadOnlyComposable get() = palette.brandCyan
val PpBrandGreen: Color
    @Composable @ReadOnlyComposable get() = palette.brandGreen
val PpBrandYellow: Color
    @Composable @ReadOnlyComposable get() = palette.brandYellow

val PpBg: Color
    @Composable @ReadOnlyComposable get() = palette.bg
val PpBgElevated: Color
    @Composable @ReadOnlyComposable get() = palette.bgElevated
val PpSurface: Color
    @Composable @ReadOnlyComposable get() = palette.surface
val PpSurfaceMuted: Color
    @Composable @ReadOnlyComposable get() = palette.surfaceMuted
val PpSurfaceInput: Color
    @Composable @ReadOnlyComposable get() = palette.surfaceInput
val PpBorder: Color
    @Composable @ReadOnlyComposable get() = palette.border
val PpDivider: Color
    @Composable @ReadOnlyComposable get() = palette.divider
val PpText: Color
    @Composable @ReadOnlyComposable get() = palette.text
val PpTextMuted: Color
    @Composable @ReadOnlyComposable get() = palette.textMuted
val PpTextSubtle: Color
    @Composable @ReadOnlyComposable get() = palette.textSubtle
val PpHeading: Color
    @Composable @ReadOnlyComposable get() = palette.heading

val PpAccent: Color
    @Composable @ReadOnlyComposable get() = palette.accent
val PpAccentHover: Color
    @Composable @ReadOnlyComposable get() = palette.accentHover
val PpAccentSoft: Color
    @Composable @ReadOnlyComposable get() = palette.accentSoft
val PpAccentGlow: Color
    @Composable @ReadOnlyComposable get() = palette.accentGlow

val PpSecondary: Color
    @Composable @ReadOnlyComposable get() = palette.secondary
val PpSecondarySoft: Color
    @Composable @ReadOnlyComposable get() = palette.secondarySoft

val PpPlay: Color
    @Composable @ReadOnlyComposable get() = palette.play

val PpDanger: Color
    @Composable @ReadOnlyComposable get() = palette.danger
val PpHighlight: Color
    @Composable @ReadOnlyComposable get() = palette.highlight
val PpGold: Color
    @Composable @ReadOnlyComposable get() = palette.gold
val PpInfo: Color
    @Composable @ReadOnlyComposable get() = palette.info
val PpWarning: Color
    @Composable @ReadOnlyComposable get() = palette.warning
val PpPurple: Color
    @Composable @ReadOnlyComposable get() = palette.purple

val PpGlassSurface: Color
    @Composable @ReadOnlyComposable get() = palette.glassSurface
val PpGlassBorder: Color
    @Composable @ReadOnlyComposable get() = palette.glassBorder
val PpNeonGreen: Color
    @Composable @ReadOnlyComposable get() = palette.neonGreen
val PpNeonCyan: Color
    @Composable @ReadOnlyComposable get() = palette.neonCyan
