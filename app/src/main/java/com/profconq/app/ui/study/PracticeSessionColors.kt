package com.profconq.app.ui.study

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Practice session surfaces. Control accents match the Port neon green / cyan system.
 */
object PracticeSessionColors {
    val Bg = Color(0xFF050B18)
    val BgElev = Color(0xFF0A1224)
    val Card = Color(0xFF101E36)
    val Card2 = Color(0xFF142440)
    val ButtonSurface = Card
    val Border = Color(0x404A7BC4)
    val BorderStrong = Color(0x584A7BC4)
    val TextPrimary = Color(0xFFE8EEF8)
    val TextMuted = Color(0xFF8BA3C7)
    val TextFaint = Color(0xFF5C7294)
    val Accent = Color(0xFF7EE887)
    val AccentSoft = Color(0x337EE887)
    val Danger = Color(0xFFEF4444)
    val Info = Color(0xFF00D2FF)
    val OnAccent = Color(0xFF0A1E45)

    val CardGradient = Brush.verticalGradient(listOf(Card, Card2))
    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF7EE887), Color(0xFF00D2FF)),
    )
}
