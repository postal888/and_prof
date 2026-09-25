package com.profconq.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.R
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import com.profconq.app.ui.theme.rememberScreenGradientBrush

object PortLayout {
    val Gutter = 16.dp
    val HeaderHeight = 64.dp
    val HeaderLeading = 40.dp
    val HeaderLogo = 44.dp
    val HeaderTrail = 80.dp
    val HeaderButton = 40.dp
    val HeaderToContent = 12.dp
}

private val DefaultTabHeaderLogoSize = PortLayout.HeaderLogo

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
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PortLayout.HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showLogo) {
            Box(
                modifier = Modifier.size(PortLayout.HeaderLogo),
                contentAlignment = Alignment.Center,
            ) {
                ProfconqLogo(size = logoSize)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = PpHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 24.sp,
            )
            Text(
                text = subtitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = PpTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier.height(16.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            content = actions,
        )
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(PortLayout.HeaderButton),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.commonBack,
                    tint = PpHeading,
                    modifier = Modifier.size(22.dp),
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
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val strings = LocalUiStrings.current
    ProfconqBrandHeader(
        title = tab.label(strings),
        subtitle = subtitle,
        modifier = modifier,
        logoSize = logoSize,
        onBack = onBack,
        actions = actions,
    )
}

/** Same layout as [TabScreenHeader] when the screen is not a main tab (e.g. Studio, Progress). */
@Composable
fun TabScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    logoSize: Dp = DefaultTabHeaderLogoSize,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    ProfconqBrandHeader(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        logoSize = logoSize,
        onBack = onBack,
        actions = actions,
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

/** Screen background — deep navy with subtle vertical gradient (design template). */
@Composable
fun Modifier.portScreenBackground(): Modifier = background(rememberScreenGradientBrush())
