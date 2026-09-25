package com.profconq.app.ui.screens.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.profconq.app.ui.components.dragAndTap
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpTextMuted

@Composable
fun YouTubeBackgroundBar(
    title: String?,
    onOpenVideoTab: () -> Unit,
    onDrag: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .dragAndTap(onDrag = onDrag, onTap = onOpenVideoTab)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = PpAccent,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title?.takeIf { it.isNotBlank() } ?: strings.youtubeBackgroundPlaying,
            style = MaterialTheme.typography.bodyMedium,
            color = PpHeading,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = strings.youtubeBackgroundOpen,
            style = MaterialTheme.typography.labelMedium,
            color = PpAccent,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PpTextMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}
