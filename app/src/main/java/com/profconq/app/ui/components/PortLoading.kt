package com.profconq.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.profconq.app.ui.theme.PpGlassBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush

/** Spinner with green→cyan arc (design template). */
@Composable
fun GradientCircularLoader(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    strokeWidth: Dp = 3.dp,
) {
    val gradient = rememberAccentGradientBrush()
    val trackColor = PpGlassBorder.copy(alpha = 0.35f)
    val transition = rememberInfiniteTransition(label = "loaderSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loaderRotation",
    )
    Canvas(
        modifier = modifier
            .size(size)
            .rotate(rotation),
    ) {
        val stroke = strokeWidth.toPx()
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = gradient,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/** Indeterminate horizontal bar with sliding gradient segment. */
@Composable
fun GradientLoadingBar(
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    val gradient = rememberAccentGradientBrush()
    val trackColor = PpGlassBorder.copy(alpha = 0.3f)
    val transition = rememberInfiniteTransition(label = "barSlide")
    val offsetFraction by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "barOffset",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(trackColor),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val barWidth = maxWidth * 0.35f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(barWidth)
                    .offset(x = maxWidth * offsetFraction)
                    .clip(RoundedCornerShape(99.dp))
                    .background(gradient),
            )
        }
    }
}

/** Centered loader with optional caption — subtitles, books, import, etc. */
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier,
    message: String? = null,
    showBar: Boolean = true,
    loaderSize: Dp = 36.dp,
) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GradientCircularLoader(size = loaderSize)
        if (showBar) {
            GradientLoadingBar(
                modifier = Modifier
                    .width(160.dp)
                    .padding(horizontal = 8.dp),
            )
        }
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = PpTextMuted,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Full-area overlay while content loads (e.g. opening a book). */
@Composable
fun LoadingOverlay(
    visible: Boolean,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.profconq.app.ui.theme.PpBg.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .glassCard(cornerRadius = 20.dp)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LoadingContent(message = message, loaderSize = 40.dp)
        }
    }
}
