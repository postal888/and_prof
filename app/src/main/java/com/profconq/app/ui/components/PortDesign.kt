package com.profconq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpGlassBorder
import com.profconq.app.ui.theme.PpGlassSurface
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpNeonCyan
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import com.profconq.app.ui.theme.rememberGlassBorderBrush

private val GlassShape = RoundedCornerShape(16.dp)

@Composable
fun Modifier.glassCard(
    cornerRadius: Dp = 16.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = rememberGlassBorderBrush()
    return this
        .clip(shape)
        .background(PpGlassSurface)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    compact: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val gradient = rememberAccentGradientBrush()
    val shape = PortButtonShape
    val interactive = enabled && !loading
    Box(
        modifier = modifier
            .height(if (compact) PortCompactHeight else PortButtonHeight)
            .then(
                if (interactive) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        spotColor = PpNeonGreen.copy(alpha = 0.55f),
                        ambientColor = PpNeonCyan.copy(alpha = 0.35f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(
                if (interactive) gradient else Brush.horizontalGradient(listOf(PpTextMuted, PpTextMuted)),
            )
            .portClickable(enabled = interactive, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            GradientCircularLoader(size = 28.dp, strokeWidth = 3.dp)
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leading?.invoke()
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = PpBrandNavy,
                )
            }
        }
    }
}

@Composable
fun GlassOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = PortButtonShape
    val borderBrush = rememberGlassBorderBrush()
    Box(
        modifier = modifier
            .height(if (compact) PortCompactHeight else PortButtonHeight)
            .clip(shape)
            .background(PpSurfaceInput.copy(alpha = 0.65f))
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .portClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) PpNeonGreen else PpTextMuted,
            )
        }
    }
}

@Composable
fun GradientProgressBar(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val gradient = rememberAccentGradientBrush()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = PpTextMuted,
            )
            Text(
                text = "${(clamped * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = PpHeading,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(PpGlassBorder.copy(alpha = 0.35f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(gradient),
            )
        }
    }
}

@Composable
fun CircularGradientProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 7.dp,
    showLabel: Boolean = true,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val gradient = rememberAccentGradientBrush()
    val trackColor = PpGlassBorder.copy(alpha = 0.4f)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        if (showLabel) {
            Text(
                text = "${(clamped * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PpHeading,
            )
        }
    }
}
