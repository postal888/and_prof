package com.profconq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.profconq.app.studio.StudioNowPlaying
import com.profconq.app.studio.StudioNowPlayingHub
import com.profconq.app.ui.screens.youtube.YouTubeBackgroundBar
import com.profconq.app.ui.studio.StudioBackgroundBar
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBgElevated
import com.profconq.app.ui.theme.PpTextMuted
import kotlin.math.roundToInt

@Composable
fun FloatingNowPlayingOverlay(
    showStudio: Boolean,
    studio: StudioNowPlaying,
    onOpenStudio: () -> Unit,
    showYoutube: Boolean,
    youtubeTitle: String?,
    onOpenYoutube: () -> Unit,
    onDismissStudio: () -> Unit = { StudioNowPlayingHub.commands?.stop() },
    modifier: Modifier = Modifier,
) {
    if (!showStudio && !showYoutube) return
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var childSize by remember { mutableStateOf(IntSize.Zero) }
    val dismissThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    val onDrag: (Offset) -> Unit = { delta ->
        val limits = dragLimits(parentSize, childSize, dismissThresholdPx)
        offsetX = (offsetX + delta.x).coerceIn(limits.minX, limits.maxX)
        offsetY = (offsetY + delta.y).coerceIn(limits.minY, limits.maxY)
    }

    val finishDrag: () -> Unit = {
        if (showStudio && offsetY > dismissThresholdPx * 0.55f) {
            onDismissStudio()
            offsetY = 0f
        } else {
            val limits = dragLimits(parentSize, childSize, dismissThresholdPx)
            offsetY = offsetY.coerceIn(limits.minY, 0f)
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = PortLayout.Gutter, vertical = 8.dp)
            .widthIn(max = 360.dp)
            .onGloballyPositioned { coords ->
                childSize = coords.size
                coords.parentLayoutCoordinates?.size?.let { parentSize = it }
            }
            .offset {
                val limits = dragLimits(parentSize, childSize, dismissThresholdPx)
                IntOffset(
                    offsetX.coerceIn(limits.minX, limits.maxX).roundToInt(),
                    offsetY.coerceIn(limits.minY, limits.maxY).roundToInt(),
                )
            }
            .shadow(16.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(PpBgElevated)
            .border(1.dp, PpAccent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .dragAndTap(onDrag = onDrag, onTap = {}, onDragEnd = finishDrag),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 4.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(PpTextMuted.copy(alpha = 0.7f)),
        )
        if (showStudio) {
            StudioBackgroundBar(
                nowPlaying = studio,
                onOpenStudio = onOpenStudio,
                onDismiss = onDismissStudio,
                onDrag = onDrag,
                onDragEnd = finishDrag,
            )
        }
        if (showYoutube) {
            YouTubeBackgroundBar(
                title = youtubeTitle,
                onOpenVideoTab = onOpenYoutube,
                onDrag = onDrag,
            )
        }
    }
}

private data class DragLimits(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
)

private fun dragLimits(parent: IntSize, child: IntSize, dismissOverscroll: Float = 0f): DragLimits {
    if (parent.width <= 0 || parent.height <= 0 || child.width <= 0 || child.height <= 0) {
        return DragLimits(0f, 0f, 0f, 0f)
    }
    val extraX = ((parent.width - child.width) / 2f).coerceAtLeast(0f)
    val extraY = (parent.height - child.height).toFloat().coerceAtLeast(0f)
    return DragLimits(
        minX = -extraX,
        maxX = extraX,
        minY = -extraY,
        maxY = dismissOverscroll.coerceAtLeast(0f),
    )
}

fun Modifier.dragAndTap(
    onDrag: (Offset) -> Unit,
    onTap: () -> Unit,
    onDragEnd: () -> Unit = {},
): Modifier = composed {
    val dragHandler = rememberUpdatedState(onDrag)
    val tapHandler = rememberUpdatedState(onTap)
    val endHandler = rememberUpdatedState(onDragEnd)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            var dragged = false
            val slop = viewConfiguration.touchSlop
            val origin = down.position
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.pressed) {
                    val travel = change.position - origin
                    if (!dragged && travel.getDistance() > slop) {
                        dragged = true
                    }
                    if (dragged) {
                        val step = change.positionChange()
                        if (step != Offset.Zero) {
                            dragHandler.value(step)
                            change.consume()
                        }
                    }
                } else {
                    if (dragged) endHandler.value() else tapHandler.value()
                    break
                }
            }
        }
    }
}
