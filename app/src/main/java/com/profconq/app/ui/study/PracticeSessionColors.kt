package com.profconq.app.ui.study

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Practice session palette — forest green accent, dark outlined buttons. */
object PracticeSessionColors {
    val Bg = Color(0xFF0A0A0F)
    val BgElev = Color(0xFF14141C)
    val Card = Color(0xFF1A1A26)
    val Card2 = Color(0xFF20202E)
    val ButtonSurface = Card
    val Border = Color(0x14FFFFFF)
    val BorderStrong = Color(0x24FFFFFF)
    val TextPrimary = Color(0xFFF8F9FC)
    val TextMuted = Color(0xFF9CA3AF)
    val TextFaint = Color(0xFF6B7280)
    val Accent = Color(0xFF2E9348)
    val AccentSoft = Color(0x262E9348)
    val Danger = Color(0xFFFF5566)
    val Info = Color(0xFF5B8DEF)
    val OnAccent = Color(0xFF2E9348)

    val CardGradient = Brush.verticalGradient(listOf(Card, Card2))
    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF2E9348), Color(0xFF26803F)),
    )
}
