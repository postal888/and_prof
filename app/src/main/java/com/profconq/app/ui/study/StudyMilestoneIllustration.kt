package com.profconq.app.ui.study

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class StudyMilestoneStyle {
    /** «На сегодня по расписанию всё» — календарь + галочка. */
    ScheduleClear,
    /** Итог сессии — кольцо прогресса + галочка. */
    SessionComplete,
}

@Composable
fun StudyMilestoneIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    style: StudyMilestoneStyle = StudyMilestoneStyle.ScheduleClear,
    animateIn: Boolean = true,
) {
    var visible by remember { mutableStateOf(!animateIn) }
    LaunchedEffect(animateIn) {
        if (animateIn) visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "milestoneScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "milestoneAlpha",
    )

    Canvas(
        modifier = modifier.size(size),
    ) {
        val s = scale
        val a = alpha
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PracticeSessionColors.Accent.copy(alpha = 0.28f * a),
                    PracticeSessionColors.Info.copy(alpha = 0.12f * a),
                    Color.Transparent,
                ),
                center = Offset(cx, cy * 0.92f),
                radius = w * 0.52f * s,
            ),
            radius = w * 0.52f * s,
            center = Offset(cx, cy * 0.92f),
        )

        when (style) {
            StudyMilestoneStyle.ScheduleClear -> drawScheduleClearHero(cx, cy, w, h, s, a)
            StudyMilestoneStyle.SessionComplete -> drawSessionCompleteHero(cx, cy, w, h, s, a)
        }
    }
}

private fun DrawScope.drawScheduleClearHero(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    scale: Float,
    alpha: Float,
) {
    val cardW = w * 0.56f * scale
    val cardH = h * 0.38f * scale
    val cardLeft = cx - cardW / 2f
    val cardTop = cy - cardH * 0.35f

    drawRoundRect(
        color = PracticeSessionColors.Card2.copy(alpha = 0.9f * alpha),
        topLeft = Offset(cardLeft + 10f * scale, cardTop + 12f * scale),
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(18f * scale, 18f * scale),
    )
    drawRoundRect(
        brush = PracticeSessionColors.CardGradient,
        topLeft = Offset(cardLeft, cardTop),
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(18f * scale, 18f * scale),
        alpha = alpha,
    )
    drawRoundRect(
        color = PracticeSessionColors.BorderStrong.copy(alpha = 0.85f * alpha),
        topLeft = Offset(cardLeft, cardTop),
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(18f * scale, 18f * scale),
        style = Stroke(width = 1.5f * scale),
    )

    val calW = cardW * 0.72f
    val calH = cardH * 0.62f
    val calLeft = cx - calW / 2f
    val calTop = cardTop + cardH * 0.22f
    drawRoundRect(
        color = PracticeSessionColors.BgElev.copy(alpha = 0.95f * alpha),
        topLeft = Offset(calLeft, calTop),
        size = Size(calW, calH),
        cornerRadius = CornerRadius(10f * scale, 10f * scale),
    )
    drawRoundRect(
        brush = PracticeSessionColors.AccentGradient,
        topLeft = Offset(calLeft, calTop),
        size = Size(calW, calH * 0.28f),
        cornerRadius = CornerRadius(10f * scale, 10f * scale),
        alpha = alpha,
    )
    val dotR = 3.2f * scale
    val gridTop = calTop + calH * 0.38f
    val colW = calW / 4f
    for (row in 0 until 2) {
        for (col in 0 until 4) {
            val dotCx = calLeft + colW * (col + 0.5f)
            val dotCy = gridTop + calH * 0.22f * row
            val filled = row == 0 && col < 3
            drawCircle(
                color = if (filled) {
                    PracticeSessionColors.Accent.copy(alpha = 0.85f * alpha)
                } else {
                    PracticeSessionColors.TextFaint.copy(alpha = 0.35f * alpha)
                },
                radius = dotR,
                center = Offset(dotCx, dotCy),
            )
        }
    }

    drawBadgeCheck(cx, cardTop + cardH + 8f * scale, w * 0.19f * scale, alpha)

    drawSparkle(Offset(cx - w * 0.34f, cy - h * 0.28f), 4f * scale, alpha)
    drawSparkle(Offset(cx + w * 0.36f, cy - h * 0.22f), 3f * scale, alpha * 0.85f)
    drawSparkle(Offset(cx + w * 0.3f, cy + h * 0.32f), 2.5f * scale, alpha * 0.7f)
}

private fun DrawScope.drawSessionCompleteHero(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    scale: Float,
    alpha: Float,
) {
    val ringR = w * 0.28f * scale
    val stroke = 5f * scale
    drawCircle(
        color = PracticeSessionColors.BorderStrong.copy(alpha = 0.5f * alpha),
        radius = ringR,
        center = Offset(cx, cy),
        style = Stroke(width = stroke),
    )
    drawArc(
        brush = PracticeSessionColors.AccentGradient,
        startAngle = -90f,
        sweepAngle = 300f,
        useCenter = false,
        topLeft = Offset(cx - ringR, cy - ringR),
        size = Size(ringR * 2f, ringR * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
        alpha = alpha,
    )
    drawBadgeCheck(cx, cy, ringR * 0.55f, alpha)
    drawSparkle(Offset(cx - ringR * 1.15f, cy - ringR * 0.9f), 3.5f * scale, alpha)
    drawSparkle(Offset(cx + ringR * 1.2f, cy - ringR * 0.75f), 3f * scale, alpha * 0.8f)
}

private fun DrawScope.drawBadgeCheck(cx: Float, cy: Float, radius: Float, alpha: Float) {
    drawCircle(
        brush = PracticeSessionColors.AccentGradient,
        radius = radius,
        center = Offset(cx, cy),
        alpha = alpha,
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.18f * alpha),
        radius = radius,
        center = Offset(cx, cy + radius * 0.08f),
    )
    val check = Path().apply {
        val r = radius * 0.42f
        moveTo(cx - r * 0.95f, cy + r * 0.05f)
        lineTo(cx - r * 0.15f, cy + r * 0.85f)
        lineTo(cx + r * 1.05f, cy - r * 0.75f)
    }
    drawPath(
        path = check,
        color = PracticeSessionColors.OnAccent.copy(alpha = alpha),
        style = Stroke(
            width = radius * 0.2f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        color = PracticeSessionColors.Info.copy(alpha = 0.9f * alpha),
        radius = radius,
        center = center,
    )
    rotate(degrees = 45f, pivot = center) {
        drawRoundRect(
            color = PracticeSessionColors.Accent.copy(alpha = 0.55f * alpha),
            topLeft = Offset(center.x - radius * 0.35f, center.y - radius * 2.2f),
            size = Size(radius * 0.7f, radius * 4.4f),
            cornerRadius = CornerRadius(radius * 0.35f, radius * 0.35f),
        )
    }
}
