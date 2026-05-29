package com.proficon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.proficon.app.media.AudioPlayerController
import com.proficon.app.media.formatAudioTime
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.theme.PpPlay
import com.proficon.app.ui.theme.PpSecondary
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpSurfaceInput
import com.proficon.app.ui.theme.PpText
import com.proficon.app.ui.theme.PpTextMuted
import kotlinx.coroutines.delay

@Composable
fun rememberAudioPlayerController(audioPath: String?): AudioPlayerController {
    val controller = remember { AudioPlayerController() }
    LaunchedEffect(audioPath) {
        controller.load(audioPath)
    }
    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }
    return controller
}

@Composable
fun CompactAudioPlayerBar(
    audioPath: String?,
    title: String = "Запись папки",
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (audioPath.isNullOrBlank()) return

    val player = rememberAudioPlayerController(audioPath)

    LaunchedEffect(player.isPlaying) {
        while (player.isPlaying) {
            player.refreshPosition()
            delay(200)
        }
    }

    if (!player.hasAudio) {
        MutedText(
            text = "Аудиофайл не найден",
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PpSurfaceInput)
                .padding(12.dp),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = PpHeading,
            )
            Text(
                text = "${formatAudioTime(player.positionMs)} / ${formatAudioTime(player.durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = PpTextMuted,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = { player.togglePlayPause() },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (player.isPlaying) "Пауза" else "Воспроизвести",
                    tint = PpPlay,
                )
            }
            IconButton(
                onClick = { player.stop() },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Стоп",
                    tint = PpTextMuted,
                )
            }
            Slider(
                value = if (player.durationMs > 0) {
                    player.positionMs.toFloat() / player.durationMs.toFloat()
                } else {
                    0f
                },
                onValueChange = { fraction ->
                    val target = (fraction * player.durationMs).toInt()
                    player.seekTo(target)
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PpSecondary,
                    activeTrackColor = PpSecondary,
                    inactiveTrackColor = PpTextMuted.copy(alpha = 0.25f),
                ),
            )
            if (onShare != null) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Экспорт",
                        tint = PpTextMuted,
                    )
                }
            }
        }
    }
}
