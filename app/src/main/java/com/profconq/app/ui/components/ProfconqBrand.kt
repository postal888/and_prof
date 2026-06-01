package com.profconq.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.profconq.app.R
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import com.profconq.app.ui.theme.LocalPortPalette

private val DefaultTabHeaderLogoSize = 44.dp

@Composable
fun ProfconqLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Image(
        painter = painterResource(R.drawable.profconq_logo),
        contentDescription = "Profconq",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.18f)),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun ProfconqBrandHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    logoSize: Dp = DefaultTabHeaderLogoSize,
    showLogo: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showLogo) {
            ProfconqLogo(size = logoSize)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = PpHeading,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PpTextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Tab title from bottom navigation + optional hint under it. */
@Composable
fun TabScreenHeader(
    tab: MainTab,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    logoSize: Dp = DefaultTabHeaderLogoSize,
) {
    val strings = LocalUiStrings.current
    ProfconqBrandHeader(
        title = tab.label(strings),
        subtitle = subtitle,
        modifier = modifier,
        logoSize = logoSize,
    )
}

/** Same layout as [TabScreenHeader] when the screen is not a main tab (e.g. Progress). */
@Composable
fun TabScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    logoSize: Dp = DefaultTabHeaderLogoSize,
) {
    ProfconqBrandHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        logoSize = logoSize,
    )
}

/** Accent line — green → cyan from logo. */
@Composable
fun BrandAccentDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(rememberAccentGradientBrush()),
    )
}

/** Screen background (solid — gradients can render black on some emulators). */
@Composable
fun Modifier.portScreenBackground(): Modifier = background(LocalPortPalette.current.bg)
