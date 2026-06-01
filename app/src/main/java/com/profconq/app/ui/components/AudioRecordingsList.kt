package com.profconq.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Scrollable list of audio rows (~2 visible at once). */
@Composable
fun AudioRecordingsList(
    audioPaths: List<String>,
    titleForPath: (path: String, index: Int) -> String,
    onShare: (String) -> Unit,
    onTrim: (String) -> Unit,
    onTitleChange: ((path: String, title: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxVisibleRows: Int = 2,
) {
    if (audioPaths.isEmpty()) return

    val rowHeight = 52.dp
    val spacing = 6.dp
    val maxHeight = rowHeight * maxVisibleRows + spacing * (maxVisibleRows - 1).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        itemsIndexed(audioPaths, key = { _, path -> path }) { index, path ->
            CompactAudioPlayerBar(
                audioPath = path,
                title = titleForPath(path, index),
                onTitleChange = onTitleChange?.let { callback -> { title -> callback(path, title) } },
                onShare = { onShare(path) },
                onTrim = { onTrim(path) },
            )
        }
    }
}
