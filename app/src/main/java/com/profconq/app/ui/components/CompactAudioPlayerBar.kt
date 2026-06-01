package com.profconq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.profconq.app.media.AudioPlayerController
import com.profconq.app.media.formatAudioTime
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpPlay
import com.profconq.app.ui.theme.PpSecondary
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
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
    onTitleChange: ((String) -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onTrim: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (audioPath.isNullOrBlank()) return

    val strings = LocalUiStrings.current
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
                .clip(RoundedCornerShape(8.dp))
                .background(PpSurfaceInput)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PpSurfaceInput)
            .padding(start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        IconButton(
            onClick = { player.togglePlayPause() },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (player.isPlaying) "Пауза" else "Воспроизвести",
                tint = PpPlay,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = { player.stop() },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Стоп",
                tint = PpTextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        RecordingTitleField(
            title = title,
            onTitleChange = onTitleChange,
            modifier = Modifier.widthIn(max = 88.dp),
        )
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
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = PpSecondary,
                activeTrackColor = PpSecondary,
                inactiveTrackColor = PpTextMuted.copy(alpha = 0.25f),
            ),
        )
        Text(
            text = "${formatAudioTime(player.positionMs)}/${formatAudioTime(player.durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = PpTextMuted,
            modifier = Modifier.widthIn(min = 56.dp),
            maxLines = 1,
        )
        if (onTrim != null) {
            IconButton(
                onClick = onTrim,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = strings.audioTrimEdit,
                    tint = PpTextMuted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        if (onShare != null) {
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Экспорт",
                    tint = PpTextMuted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordingTitleField(
    title: String,
    onTitleChange: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onTitleChange == null) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = PpHeading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    var draft by rememberSaveable(title) { mutableStateOf(title) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(title) {
        if (!editing) draft = title
    }
    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    if (editing) {
        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = modifier
                .focusRequester(focusRequester)
                .clip(RoundedCornerShape(4.dp))
                .background(PpSurfaceInput.copy(alpha = 0.65f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .onFocusChanged { focus ->
                    if (!focus.isFocused) {
                        editing = false
                        if (draft.trim() != title.trim()) {
                            onTitleChange(draft.trim())
                        } else {
                            draft = title
                        }
                    }
                },
            textStyle = TextStyle(
                color = PpText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            ),
            cursorBrush = SolidColor(PpSecondary),
            singleLine = true,
            maxLines = 1,
        )
    } else {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = PpHeading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.clickable { editing = true },
        )
    }
}
