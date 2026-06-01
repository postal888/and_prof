package com.profconq.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.profconq.app.data.model.AppSettings
import com.profconq.app.data.model.AppThemeMode

@Composable
fun PortTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPortPalette else LightPortPalette
    val colorScheme = remember(palette) { palette.toColorScheme() }
    val typography = palette.toTypography()

    CompositionLocalProvider(LocalPortPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

@Composable
fun PortTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    PortTheme(darkTheme = settings.themeMode != AppThemeMode.Light, content = content)
}
