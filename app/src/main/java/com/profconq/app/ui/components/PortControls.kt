package com.profconq.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.ui.theme.PpBgElevated
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpNeonCyan
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import com.profconq.app.ui.theme.rememberGlassBorderBrush

val PortButtonShape = RoundedCornerShape(16.dp)
val PortChipShape = RoundedCornerShape(12.dp)
val PortButtonHeight = 52.dp
val PortCompactHeight = 44.dp
private val PortChipHeight = 36.dp
private const val PortPressScale = 0.97f

@Composable
fun Modifier.portClickable(
    enabled: Boolean = true,
    boundedRipple: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) PortPressScale else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.62f),
        label = "portPressScale",
    )
    val rippleColor = PpNeonCyan
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = ripple(bounded = boundedRipple, color = rippleColor),
            onClick = onClick,
        )
}

@Composable
fun rememberPortPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) PortPressScale else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.62f),
        label = "portRememberedPress",
    )
    return scale
}

enum class PortTextEmphasis { Accent, Muted, Danger }

@Composable
fun PortTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasis: PortTextEmphasis = PortTextEmphasis.Accent,
    leading: (@Composable () -> Unit)? = null,
) {
    val color = when (emphasis) {
        PortTextEmphasis.Accent -> PpNeonGreen
        PortTextEmphasis.Muted -> PpTextMuted
        PortTextEmphasis.Danger -> PpDanger
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .portClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        leading?.invoke()
        Text(
            text = text,
            color = if (enabled) color else color.copy(alpha = 0.4f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.height(PortChipHeight),
    enabled: Boolean = true,
    compact: Boolean = true,
) {
    val shape = PortChipShape
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.78f),
        label = "portChipGlow",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> PpTextMuted.copy(alpha = 0.4f)
            selected -> PpBrandNavy
            else -> PpTextMuted
        },
        label = "portChipText",
    )
    val gradient = rememberAccentGradientBrush()
    val idle = Brush.verticalGradient(listOf(PpSurfaceInput, PpBgElevated))
    Box(
        modifier = modifier
            .shadow(
                elevation = (10f * glow).dp,
                shape = shape,
                ambientColor = PpNeonCyan.copy(alpha = 0.45f * glow),
                spotColor = PpNeonGreen.copy(alpha = 0.4f * glow),
            )
            .clip(shape)
            .background(if (selected) gradient else idle, shape)
            .border(
                width = 1.dp,
                brush = if (selected) {
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.28f), PpNeonCyan.copy(alpha = 0.35f)),
                    )
                } else {
                    rememberGlassBorderBrush()
                },
                shape = shape,
            )
            .portClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (compact) 10.dp else 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (compact) 12.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PortSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = PortChipShape
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PortChipHeight)
            .clip(shape)
            .background(PpBgElevated)
            .border(1.dp, rememberGlassBorderBrush(), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items.forEachIndexed { index, label ->
            PortChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                enabled = enabled,
                compact = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}
