package com.profconq.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlin.math.abs
import kotlin.math.roundToInt
import com.profconq.app.media.AudioPcmDecoder
import com.profconq.app.media.AudioPcmUtils
import com.profconq.app.media.AudioPlayerController
import com.profconq.app.media.AudioTrimExporter
import com.profconq.app.media.formatAudioTime
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpPlay
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
sealed class AudioTrimResult {
    data class Saved(val path: String) : AudioTrimResult()
    data object Deleted : AudioTrimResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimBottomSheet(
    audioPath: String,
    filePrefix: String,
    onDismiss: () -> Unit,
    onResult: (AudioTrimResult) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.profconq.app.ui.theme.PpSurface,
    ) {
        AudioTrimEditorContent(
            audioPath = audioPath,
            filePrefix = filePrefix,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            onCancel = onDismiss,
            onResult = onResult,
        )
    }
}

@Composable
fun AudioTrimEditorContent(
    audioPath: String,
    filePrefix: String,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onResult: (AudioTrimResult) -> Unit,
) {
    val strings = LocalUiStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember(audioPath) { mutableStateOf(true) }
    var error by remember(audioPath) { mutableStateOf<String?>(null) }
    var durationMs by remember(audioPath) { mutableStateOf(0L) }
    var peaks by remember(audioPath) { mutableStateOf<List<Float>>(emptyList()) }
    var startFrac by remember(audioPath) { mutableFloatStateOf(0f) }
    var endFrac by remember(audioPath) { mutableFloatStateOf(1f) }
    var saving by remember { mutableStateOf(false) }

    val player = remember { AudioPlayerController() }
    LaunchedEffect(audioPath) {
        player.load(audioPath)
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            player.refreshPosition()
            delay(80)
        }
    }
    LaunchedEffect(audioPath) {
        loading = true
        error = null
        val decoded = withContext(Dispatchers.IO) {
            runCatching { AudioPcmDecoder.decode(audioPath) }
        }
        decoded.fold(
            onSuccess = { pcm ->
                durationMs = AudioPcmUtils.durationMs(pcm)
                peaks = AudioPcmUtils.waveformPeaks(pcm)
                startFrac = 0f
                endFrac = 1f
                loading = false
            },
            onFailure = {
                error = strings.audioTrimLoadError
                loading = false
            },
        )
    }

    val minSpanFrac = if (durationMs > 0) {
        (AudioPcmUtils.MIN_TRIM_MS.toFloat() / durationMs).coerceIn(0.05f, 1f)
    } else {
        1f
    }

    fun startMs() = (startFrac * durationMs).roundToInt().toLong()
    fun endMs() = (endFrac * durationMs).roundToInt().toLong()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.audioTrimTitle, style = MaterialTheme.typography.titleMedium, color = PpText)

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = PpAccent, modifier = Modifier.size(32.dp))
                }
            }
            error != null -> {
                Text(error!!, color = PpTextMuted)
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatAudioTime(startMs().toInt()), color = PpAccent, style = MaterialTheme.typography.labelMedium)
                    Text(formatAudioTime(endMs().toInt()), color = PpAccent, style = MaterialTheme.typography.labelMedium)
                }

                WaveformTrimCanvas(
                    peaks = peaks,
                    startFrac = startFrac,
                    endFrac = endFrac,
                    minSpanFrac = minSpanFrac,
                    onStartFracChange = { startFrac = it },
                    onEndFracChange = { endFrac = it },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (player.isPlaying) {
                                player.stop()
                            } else {
                                player.playSegment(startMs().toInt(), endMs().toInt())
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(PpSurfaceInput),
                    ) {
                        Icon(
                            imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = strings.audioTrimPlay,
                            tint = PpPlay,
                        )
                    }
                    Text(
                        text = "${formatAudioTime(startMs().toInt())} – ${formatAudioTime(endMs().toInt())}",
                        color = PpTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    if (saving || loading || error != null) return@Button
                    scope.launch {
                        saving = true
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val start = startMs()
                                val end = endMs()
                                val needsTrim = start > 50 || end < durationMs - 50
                                if (!needsTrim) {
                                    audioPath
                                } else {
                                    AudioTrimExporter.trimAndSave(
                                        context,
                                        audioPath,
                                        start,
                                        end,
                                        filePrefix,
                                    )
                                }
                            }
                        }
                        saving = false
                        result.onSuccess { path -> onResult(AudioTrimResult.Saved(path)) }
                            .onFailure { error = strings.audioTrimSaveError }
                    }
                },
                enabled = !loading && error == null && !saving,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
            ) {
                Text(strings.audioTrimSave, color = PpHeading)
            }
            TextButton(
                onClick = { onResult(AudioTrimResult.Deleted) },
                enabled = !saving,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = PpTextMuted, modifier = Modifier.size(18.dp))
                Text(strings.audioTrimDelete, color = PpTextMuted, modifier = Modifier.padding(start = 4.dp))
            }
        }
        TextButton(onClick = onCancel, enabled = !saving, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(strings.audioTrimCancel, color = PpTextMuted)
        }
    }
}

private enum class TrimHandle { Start, End }

@Composable
private fun WaveformTrimCanvas(
    peaks: List<Float>,
    startFrac: Float,
    endFrac: Float,
    minSpanFrac: Float,
    onStartFracChange: (Float) -> Unit,
    onEndFracChange: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val touchRadiusPx = with(density) { 36.dp.toPx() }
    val handleBarWidthPx = with(density) { 4.dp.toPx() }
    val currentStartFrac by rememberUpdatedState(startFrac)
    val currentEndFrac by rememberUpdatedState(endFrac)
    val onStartChange by rememberUpdatedState(onStartFracChange)
    val onEndChange by rememberUpdatedState(onEndFracChange)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PpSurfaceInput),
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val selectedBarColor = PpAccent
        val mutedBarColor = PpTextMuted.copy(alpha = 0.35f)
        val handleColor = PpAccent

        Canvas(modifier = Modifier.matchParentSize()) {
            if (peaks.isEmpty()) return@Canvas
            val barCount = peaks.size
            val gap = 2f
            val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
            val centerY = size.height / 2f
            val maxBarHeight = size.height * 0.72f

            peaks.forEachIndexed { index, peak ->
                val x = index * (barWidth + gap)
                val barH = maxBarHeight * peak
                val frac = (index + 0.5f) / barCount
                val inSelection = frac in startFrac..endFrac
                val color = if (inSelection) selectedBarColor else mutedBarColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, centerY - barH / 2f),
                    size = Size(barWidth, barH.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }

            val selLeft = startFrac * size.width
            val selRight = endFrac * size.width
            drawRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = Offset(0f, 0f),
                size = Size(selLeft.coerceAtLeast(0f), size.height),
            )
            drawRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = Offset(selRight, 0f),
                size = Size((size.width - selRight).coerceAtLeast(0f), size.height),
            )

            val handleHeight = size.height * 0.72f
            val handleTop = centerY - handleHeight / 2f
            listOf(selLeft, selRight).forEach { handleX ->
                drawRoundRect(
                    color = handleColor,
                    topLeft = Offset(handleX - handleBarWidthPx / 2f, handleTop),
                    size = Size(handleBarWidthPx, handleHeight),
                    cornerRadius = CornerRadius(handleBarWidthPx / 2f, handleBarWidthPx / 2f),
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(widthPx, minSpanFrac, touchRadiusPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val x = down.position.x
                        val startPx = currentStartFrac * widthPx
                        val endPx = currentEndFrac * widthPx
                        val distStart = abs(x - startPx)
                        val distEnd = abs(x - endPx)
                        val handle = when {
                            distStart <= touchRadiusPx && distStart <= distEnd -> TrimHandle.Start
                            distEnd <= touchRadiusPx -> TrimHandle.End
                            else -> return@awaitEachGesture
                        }

                        down.consume()
                        drag(down.id) { change ->
                            change.consume()
                            val frac = (change.position.x / widthPx).coerceIn(0f, 1f)
                            when (handle) {
                                TrimHandle.Start -> {
                                    val maxStart = (currentEndFrac - minSpanFrac).coerceAtLeast(0f)
                                    onStartChange(frac.coerceIn(0f, maxStart))
                                }
                                TrimHandle.End -> {
                                    val minEnd = (currentStartFrac + minSpanFrac).coerceAtMost(1f)
                                    onEndChange(frac.coerceIn(minEnd, 1f))
                                }
                            }
                        }
                    }
                },
        )
    }
}
