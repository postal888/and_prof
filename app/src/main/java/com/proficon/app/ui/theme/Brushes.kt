package com.proficon.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

@Composable
fun rememberBrandGradientBrush(): Brush {
    val palette = LocalPortPalette.current
    return remember(palette) {
        Brush.linearGradient(
            colors = listOf(
                palette.gradientTop,
                palette.gradientMid,
                palette.gradientBottom,
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 700f),
        )
    }
}

@Composable
fun rememberAccentGradientBrush(): Brush {
    val palette = LocalPortPalette.current
    return remember(palette) {
        Brush.horizontalGradient(
            colors = listOf(palette.brandGreen, palette.brandCyan),
        )
    }
}
