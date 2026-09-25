package com.profconq.app.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.profconq.app.R
import com.profconq.app.studio.StudioNowPlaying
import com.profconq.app.studio.StudioNowPlayingHub
import com.profconq.app.ui.components.dragAndTap
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted

@Composable
fun StudioBackgroundBar(
    nowPlaying: StudioNowPlaying,
    onOpenStudio: () -> Unit,
    onDismiss: () -> Unit = { StudioNowPlayingHub.commands?.stop() },
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Transparent)
            .dragAndTap(onDrag = onDrag, onTap = onOpenStudio, onDragEnd = onDragEnd)
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PpAccent.copy(alpha = 0.16f)),
        ) {
            AsyncImage(
                model = nowPlaying.artwork,
                contentDescription = null,
                placeholder = painterResource(R.drawable.profconq_logo),
                error = painterResource(R.drawable.profconq_logo),
                fallback = painterResource(R.drawable.profconq_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nowPlaying.title.ifBlank { strings.studioBackgroundPlaying },
                style = MaterialTheme.typography.bodyMedium,
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(nowPlaying.subtitle, nowPlaying.collection)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { strings.tabStudio },
                style = MaterialTheme.typography.labelMedium,
                color = PpTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = { StudioNowPlayingHub.commands?.playPause() },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = if (nowPlaying.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (nowPlaying.playing) strings.commonPause else strings.studioPlay,
                tint = PpAccent,
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.studioBackgroundClose,
                tint = PpTextMuted,
            )
        }
    }
}
