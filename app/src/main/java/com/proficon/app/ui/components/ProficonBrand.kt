package com.proficon.app.ui.components

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.proficon.app.R
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpTextMuted
import com.proficon.app.ui.theme.rememberAccentGradientBrush
import com.proficon.app.ui.theme.rememberBrandGradientBrush

@Composable
fun ProficonLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Image(
        painter = painterResource(R.drawable.proficon_logo),
        contentDescription = "Proficon",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun ProficonBrandHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    logoSize: Dp = 48.dp,
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
            ProficonLogo(size = logoSize)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
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

/** Subtle brand gradient behind screen content. */
@Composable
fun Modifier.portScreenBackground(): Modifier =
    background(rememberBrandGradientBrush())
