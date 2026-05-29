package com.proficon.app.ui.screens.youtube

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.proficon.app.data.repository.ProficonRepository
import com.proficon.app.ui.components.MutedText
import com.proficon.app.ui.components.PortCard
import com.proficon.app.ui.components.ScreenHeader
import com.proficon.app.ui.components.translation.WordTranslationBar
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.theme.PpAccentSoft
import com.proficon.app.ui.components.portScreenBackground
import com.proficon.app.ui.theme.PpBorder
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpSurface
import com.proficon.app.ui.theme.PpSurfaceInput
import com.proficon.app.ui.theme.PpText
import com.proficon.app.ui.theme.PpTextMuted
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.proficon.app.data.model.SubtitleCursorMode
import com.proficon.app.data.model.SubtitleFontSize
import com.proficon.app.data.repository.WordNormalizer
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.style.TextAlign
import com.proficon.app.ui.i18n.LocalUiStrings
import com.proficon.app.youtube.SelectedWord
import com.proficon.app.youtube.SubtitlePhraseExtractor
import com.proficon.app.youtube.SubtitleLine
import com.proficon.app.youtube.VideoSearchFilter
import com.proficon.app.youtube.YouTubeUiState
import com.proficon.app.youtube.YouTubeVideoResult
import com.proficon.app.youtube.YouTubeWatchHistoryItem
import com.proficon.app.youtube.YouTubeViewModel
import com.proficon.app.youtube.YouTubeViewModelFactory

@Composable
fun YouTubeScreen(
    repository: ProficonRepository,
    modifier: Modifier = Modifier,
    viewModel: YouTubeViewModel = viewModel(factory = YouTubeViewModelFactory(repository)),
) {
    val state by viewModel.state.collectAsState()
    var pendingSeek by remember { mutableFloatStateOf(-1f) }
    val watchMode = state.videoId != null && state.videoId != "demo"
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.activeLineId, state.activeMatchIndex, state.subtitleQuery) {
        val seek = viewModel.seekTargetSec()
        if (seek != null) pendingSeek = seek
    }

    LaunchedEffect(state.addToast) {
        val message = state.addToast ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearAddToast()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground(),
    ) {
        if (watchMode) {
            WatchModeLayout(
                state = state,
                pendingSeek = pendingSeek,
                onPendingSeekApplied = { pendingSeek = -1f },
                onSeekTo = { pendingSeek = it },
                viewModel = viewModel,
            )
        } else {
            BrowseModeLayout(
                state = state,
                pendingSeek = pendingSeek,
                onPendingSeekApplied = { pendingSeek = -1f },
                viewModel = viewModel,
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = PpSurface,
                contentColor = PpText,
            )
        }
    }
}

@Composable
private fun WatchModeLayout(
    state: YouTubeUiState,
    pendingSeek: Float,
    onPendingSeekApplied: () -> Unit,
    onSeekTo: (Float) -> Unit,
    viewModel: YouTubeViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CollapsedSearchBar(
            expanded = state.isSearchExpanded,
            title = state.selectedVideoTitle,
            onToggle = viewModel::toggleSearchExpanded,
        )

        AnimatedVisibility(visible = state.isSearchExpanded) {
            val searchScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(searchScroll)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VideoInputSection(
                    state = state,
                    compact = true,
                    onVideoSearchChange = viewModel::setVideoSearchQuery,
                    onVideoSearchFilterChange = viewModel::setVideoSearchFilter,
                    onSearchVideos = viewModel::searchVideos,
                    onUrlChange = viewModel::setUrlInput,
                    onLoadLink = { viewModel.loadVideo() },
                    onDemo = viewModel::loadDemo,
                )
                if (state.isSearchingVideos) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = PpAccent, strokeWidth = 2.dp)
                    }
                }
                if (state.videoResults.isNotEmpty()) {
                    Text(
                        text = LocalUiStrings.current.searchResults,
                        style = MaterialTheme.typography.titleMedium,
                        color = PpHeading,
                    )
                    state.videoResults.forEach { result ->
                        VideoResultRow(result = result, onClick = { viewModel.selectSearchResult(result) })
                    }
                }
                if (state.videoResults.isEmpty()) {
                    WatchHistorySection(
                        history = state.watchHistory,
                        onOpen = viewModel::openFromHistory,
                        onDelete = viewModel::removeFromHistory,
                        onClearAll = viewModel::clearWatchHistory,
                    )
                }
                state.error?.let { MutedText(it) }
            }
        }

        val videoId = state.videoId
        if (videoId != null) {
            val playerResumeSec = remember(videoId) { state.playbackSec }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
            ) {
                YouTubePlayerSection(
                    videoId = videoId,
                    initialSeekSec = playerResumeSec,
                    pendingSeekSec = pendingSeek,
                    onSeekApplied = onPendingSeekApplied,
                    onCurrentSec = viewModel::updatePlaybackSec,
                    compact = true,
                )
            }
        }

        state.selectedWord?.let { word ->
            WordTranslationBar(
                word = word,
                canAddToDictionary = !word.isPhrase || state.phraseCopyEnabled,
                onAdd = viewModel::addSelectedWordToDictionary,
                onDismiss = viewModel::clearSelectedWord,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }

        if (state.subtitleLines.isNotEmpty() && state.phraseCopyEnabled) {
            CursorModeToggle(
                mode = state.cursorMode,
                onModeChange = viewModel::setCursorMode,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }

        if (state.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = PpAccent, strokeWidth = 2.dp)
            }
        }

        if (state.subtitleLines.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SubtitleSearchBar(
                        query = state.subtitleQuery,
                        matchCount = state.matchIndices.size,
                        activeMatch = state.activeMatchIndex,
                        onQueryChange = viewModel::setSubtitleQuery,
                        onPrevious = viewModel::previousMatch,
                        onNext = viewModel::nextMatch,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Субтитры",
                            style = MaterialTheme.typography.titleMedium,
                            color = PpHeading,
                        )
                        state.languageLabel?.let { MutedText(it) }
                    }
                }

                val visibleLines = viewModel.visibleLines()
                items(visibleLines, key = { it.id }) { line ->
                    SubtitleRow(
                        line = line,
                        query = state.subtitleQuery,
                        isActive = line.id == state.activeLineId,
                        isPlaying = state.playbackSec in line.startSec..<line.endSec,
                        subtitleFontSizeLevel = state.subtitleFontSizeLevel,
                        cursorMode = if (state.phraseCopyEnabled) {
                            state.cursorMode
                        } else {
                            SubtitleCursorMode.Tap
                        },
                        savedWords = state.savedWords,
                        onLineClick = {
                            viewModel.onLineClicked(line)
                            onSeekTo(line.startSec)
                        },
                        onWordClick = { token ->
                            val example = if (state.wordContextExampleEnabled) {
                                SubtitlePhraseExtractor.extractAroundWord(
                                    line.text,
                                    token.start,
                                    token.end,
                                )
                            } else {
                                null
                            }
                            viewModel.onWordClicked(token.cleanPt, example)
                        },
                        onPhraseSelected = { phrase ->
                            viewModel.onPhraseSelected(phrase, phrase)
                        },
                    )
                }

                if (state.subtitleQuery.isNotBlank() && visibleLines.isEmpty()) {
                    item { MutedText("Совпадений не найдено.") }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        } else if (!state.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                MutedText(state.error ?: "Субтитры для этого видео недоступны.")
            }
        }
    }
}

@Composable
private fun BrowseModeLayout(
    state: YouTubeUiState,
    pendingSeek: Float,
    onPendingSeekApplied: () -> Unit,
    viewModel: YouTubeViewModel,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            ScreenHeader(
                title = LocalUiStrings.current.youtubeScreenTitle,
                subtitle = LocalUiStrings.current.youtubeScreenSubtitle,
            )
        }

        item {
            VideoInputSection(
                state = state,
                compact = false,
                onVideoSearchChange = viewModel::setVideoSearchQuery,
                onVideoSearchFilterChange = viewModel::setVideoSearchFilter,
                onSearchVideos = viewModel::searchVideos,
                onUrlChange = viewModel::setUrlInput,
                onLoadLink = { viewModel.loadVideo() },
                onDemo = viewModel::loadDemo,
            )
        }

        if (state.isSearchingVideos) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = PpAccent, strokeWidth = 2.dp)
                }
            }
        }

        if (state.videoResults.isNotEmpty()) {
            item {
                Text(
                    text = LocalUiStrings.current.searchResults,
                    style = MaterialTheme.typography.titleMedium,
                    color = PpHeading,
                )
            }
            items(
                items = state.videoResults,
                key = { result -> "search-${result.videoId}" },
            ) { result ->
                VideoResultRow(result = result, onClick = { viewModel.selectSearchResult(result) })
            }
        }

        if (state.videoResults.isEmpty()) {
            item {
                WatchHistorySection(
                    history = state.watchHistory,
                    onOpen = viewModel::openFromHistory,
                    onDelete = viewModel::removeFromHistory,
                    onClearAll = viewModel::clearWatchHistory,
                )
            }
        }

        state.error?.let { error ->
            item {
                MutedText(error, modifier = Modifier.padding(bottom = 4.dp))
            }
        }

        if (state.subtitleLines.isNotEmpty()) {
            item {
                SubtitleSearchBar(
                    query = state.subtitleQuery,
                    matchCount = state.matchIndices.size,
                    activeMatch = state.activeMatchIndex,
                    onQueryChange = viewModel::setSubtitleQuery,
                    onPrevious = viewModel::previousMatch,
                    onNext = viewModel::nextMatch,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Субтитры (демо)",
                        style = MaterialTheme.typography.titleMedium,
                        color = PpHeading,
                    )
                    state.languageLabel?.let { MutedText(it) }
                }
            }

            val visibleLines = viewModel.visibleLines()
            items(visibleLines, key = { it.id }) { line ->
                SubtitleRow(
                    line = line,
                    query = state.subtitleQuery,
                    isActive = line.id == state.activeLineId,
                    isPlaying = false,
                    subtitleFontSizeLevel = state.subtitleFontSizeLevel,
                    cursorMode = if (state.phraseCopyEnabled) {
                        state.cursorMode
                    } else {
                        SubtitleCursorMode.Tap
                    },
                    savedWords = state.savedWords,
                    onLineClick = { viewModel.onLineClicked(line) },
                    onWordClick = { token ->
                        val example = if (state.wordContextExampleEnabled) {
                            SubtitlePhraseExtractor.extractAroundWord(
                                line.text,
                                token.start,
                                token.end,
                            )
                        } else {
                            null
                        }
                        viewModel.onWordClicked(token.cleanPt, example)
                    },
                    onPhraseSelected = { phrase -> viewModel.onPhraseSelected(phrase, phrase) },
                )
            }

            state.selectedWord?.let { word ->
                item {
                    WordTranslationBar(
                        word = word,
                        canAddToDictionary = !word.isPhrase || state.phraseCopyEnabled,
                        onAdd = viewModel::addSelectedWordToDictionary,
                        onDismiss = viewModel::clearSelectedWord,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun CollapsedSearchBar(
    expanded: Boolean,
    title: String?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PpSurface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title ?: "YouTube",
                style = MaterialTheme.typography.titleSmall,
                color = PpHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (expanded) "Свернуть поиск" else "Поиск другого видео",
                style = MaterialTheme.typography.labelMedium,
                color = PpTextMuted,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Свернуть" else "Развернуть",
            tint = PpAccent,
        )
    }
}

@Composable
private fun VideoInputSection(
    state: YouTubeUiState,
    compact: Boolean,
    onVideoSearchChange: (String) -> Unit,
    onVideoSearchFilterChange: (VideoSearchFilter) -> Unit,
    onSearchVideos: () -> Unit,
    onUrlChange: (String) -> Unit,
    onLoadLink: () -> Unit,
    onDemo: () -> Unit,
) {
    val strings = LocalUiStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!compact) {
            Text(
                text = strings.searchOnYoutube,
                style = MaterialTheme.typography.titleMedium,
                color = PpHeading,
            )
        }
        OutlinedTextField(
            value = state.videoSearchQuery,
            onValueChange = onVideoSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { MutedText(strings.searchPlaceholder) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = PpAccent)
            },
            trailingIcon = {
                if (state.isSearchingVideos) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PpAccent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    TextButton(
                        onClick = onSearchVideos,
                        enabled = state.videoSearchQuery.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(
                            text = strings.searchAction,
                            color = if (state.videoSearchQuery.isNotBlank()) PpAccent else PpTextMuted,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchVideos() }),
        )
        VideoSearchFilterRow(
            selected = state.videoSearchFilter,
            onSelect = onVideoSearchFilterChange,
        )

        CollapsibleLinkSection(
            urlInput = state.urlInput,
            onUrlChange = onUrlChange,
            onLoadLink = onLoadLink,
        )

        if (!compact) {
            TextButton(onClick = onDemo) {
                Text(strings.demoSubtitles, color = PpTextMuted)
            }
        }
    }
}

@Composable
private fun VideoSearchFilterRow(
    selected: VideoSearchFilter,
    onSelect: (VideoSearchFilter) -> Unit,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VideoSearchFilterButton(
            label = strings.filterVideos,
            selected = selected == VideoSearchFilter.Full,
            onClick = { onSelect(VideoSearchFilter.Full) },
            modifier = Modifier.weight(1f),
        )
        VideoSearchFilterButton(
            label = strings.filterShorts,
            selected = selected == VideoSearchFilter.Shorts,
            onClick = { onSelect(VideoSearchFilter.Shorts) },
            modifier = Modifier.weight(1f),
        )
        VideoSearchFilterButton(
            label = strings.filterAll,
            selected = selected == VideoSearchFilter.All,
            onClick = { onSelect(VideoSearchFilter.All) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VideoSearchFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PpAccentSoft else PpSurface)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (selected) PpAccent else PpTextMuted,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CollapsibleLinkSection(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onLoadLink: () -> Unit,
) {
    val strings = LocalUiStrings.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.pasteLink,
                style = MaterialTheme.typography.bodyMedium,
                color = PpTextMuted,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = PpTextMuted,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { MutedText("https://www.youtube.com/watch?v=...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onLoadLink() }),
                )
                Button(
                    onClick = onLoadLink,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PpSurface),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(strings.loadByLink, color = PpText)
                }
            }
        }
    }
}

@Composable
private fun WatchHistorySection(
    history: List<YouTubeWatchHistoryItem>,
    onOpen: (YouTubeWatchHistoryItem) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (history.isEmpty()) return

    val strings = LocalUiStrings.current
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(strings.clearHistoryTitle) },
            text = { Text(strings.clearHistoryMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    },
                ) {
                    Text(strings.clear, color = PpAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.watchHistory,
                style = MaterialTheme.typography.titleMedium,
                color = PpHeading,
            )
            TextButton(onClick = { showClearDialog = true }) {
                Text(strings.clear, color = PpTextMuted, style = MaterialTheme.typography.labelMedium)
            }
        }

        history.forEach { item ->
            WatchHistoryRow(
                item = item,
                onClick = { onOpen(item) },
                onDelete = { onDelete(item.videoId) },
            )
        }
    }
}

@Composable
private fun WatchHistoryRow(
    item: YouTubeWatchHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VideoResultRow(
            result = item.toVideoResult(),
            onClick = onClick,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "Удалить из истории", tint = PpTextMuted)
        }
    }
}

@Composable
private fun VideoResultRow(
    result: YouTubeVideoResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurface)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = result.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .width(if (result.isShort) 68.dp else 120.dp)
                .aspectRatio(if (result.isShort) 9f / 16f else 16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(PpSurfaceInput),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyMedium,
                color = PpHeading,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (result.channel.isNotBlank()) {
                Text(
                    text = result.channel,
                    style = MaterialTheme.typography.labelMedium,
                    color = PpTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            result.duration?.let { duration ->
                Text(
                    text = if (result.isShort && !duration.equals("SHORTS", ignoreCase = true)) {
                        "Short · $duration"
                    } else if (result.isShort) {
                        "Short"
                    } else {
                        duration
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = PpTextMuted,
                )
            }
        }
    }
}

@Composable
private fun SubtitleSearchBar(
    query: String,
    matchCount: Int,
    activeMatch: Int,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    PortCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { MutedText("Поиск по субтитрам…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PpTextMuted)
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить", tint = PpTextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = fieldColors(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (query.isBlank()) {
                        "Введите слово или фразу"
                    } else if (matchCount == 0) {
                        "0 совпадений"
                    } else {
                        "${activeMatch + 1} / $matchCount совпадений"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = PpTextMuted,
                )
                Row {
                    IconButton(onClick = onPrevious, enabled = matchCount > 0) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Назад", tint = PpAccent)
                    }
                    IconButton(onClick = onNext, enabled = matchCount > 0) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Вперёд", tint = PpAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CursorModeToggle(
    mode: SubtitleCursorMode,
    onModeChange: (SubtitleCursorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CursorModeButton(
            label = "👆 Слово",
            selected = mode == SubtitleCursorMode.Tap,
            onClick = { onModeChange(SubtitleCursorMode.Tap) },
            modifier = Modifier.weight(1f),
        )
        CursorModeButton(
            label = "▎ Фраза",
            selected = mode == SubtitleCursorMode.Select,
            onClick = { onModeChange(SubtitleCursorMode.Select) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CursorModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PpAccentSoft else PpSurface)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        color = if (selected) PpAccent else PpTextMuted,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun subtitleTextStyle(level: Int): TextStyle {
    val scale = SubtitleFontSize.scaleForLevel(level)
    val base = MaterialTheme.typography.bodyMedium
    return base.copy(
        fontSize = base.fontSize * scale,
        lineHeight = base.lineHeight * scale,
    )
}

@Composable
private fun subtitleTimeStyle(level: Int): TextStyle {
    val scale = SubtitleFontSize.scaleForLevel(level)
    val base = MaterialTheme.typography.labelMedium
    return base.copy(
        fontSize = base.fontSize * scale,
        lineHeight = base.lineHeight * scale,
    )
}

@Composable
private fun SubtitleRow(
    line: SubtitleLine,
    query: String,
    isActive: Boolean,
    isPlaying: Boolean,
    subtitleFontSizeLevel: Int,
    cursorMode: SubtitleCursorMode,
    savedWords: Set<String>,
    onLineClick: () -> Unit,
    onWordClick: (SubtitleWordToken) -> Unit,
    onPhraseSelected: (String) -> Unit,
) {
    val textStyle = subtitleTextStyle(subtitleFontSizeLevel)
    val timeStyle = subtitleTimeStyle(subtitleFontSizeLevel)
    val bg by animateColorAsState(
        when {
            isPlaying -> PpAccentSoft
            isActive -> PpSurfaceInput
            else -> PpSurface
        },
        label = "subtitle-bg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (cursorMode == SubtitleCursorMode.Tap) {
                    Modifier.clickable(onClick = onLineClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = formatClock(line.startSec),
            style = timeStyle,
            color = PpTextMuted,
        )
        when (cursorMode) {
            SubtitleCursorMode.Tap -> SubtitleWords(
                text = line.text,
                query = query,
                savedWords = savedWords,
                textStyle = textStyle,
                onWordClick = onWordClick,
            )
            SubtitleCursorMode.Select -> SelectableSubtitleText(
                text = line.text,
                savedWords = savedWords,
                textStyle = textStyle,
                onPhraseSelected = onPhraseSelected,
            )
        }
    }
}

@Composable
private fun SelectableSubtitleText(
    text: String,
    savedWords: Set<String>,
    textStyle: TextStyle,
    onPhraseSelected: (String) -> Unit,
) {
    var fieldValue by remember(text) { mutableStateOf(TextFieldValue(text)) }
    val selectedPhrase = remember(fieldValue, text) {
        com.proficon.app.ui.components.translation.PhraseSelectionLimits
            .extractPhrase(text, fieldValue.selection)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val selectionColors = TextSelectionColors(
            handleColor = PpAccent,
            backgroundColor = PpAccentSoft,
        )
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    val clampedSelection = com.proficon.app.ui.components.translation.PhraseSelectionLimits
                        .clampSelectionRange(
                            text = text,
                            selection = androidx.compose.ui.text.TextRange(
                                start = newValue.selection.start.coerceIn(0, text.length),
                                end = newValue.selection.end.coerceIn(0, text.length),
                            ),
                        )
                    fieldValue = TextFieldValue(text = text, selection = clampedSelection)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = textStyle.copy(color = PpText),
            )
        }
        if (selectedPhrase.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MutedText("«$selectedPhrase»", modifier = Modifier.weight(1f))
                TextButton(onClick = { onPhraseSelected(selectedPhrase) }) {
                    Text("Перевести", color = PpAccent)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubtitleWords(
    text: String,
    query: String,
    savedWords: Set<String>,
    textStyle: TextStyle,
    onWordClick: (SubtitleWordToken) -> Unit,
) {
    val tokens = remember(text) { tokenizeSubtitleText(text) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        tokens.forEach { token ->
            if (token.isWord) {
                val highlighted = query.isNotBlank() &&
                    token.cleanPt.contains(query.trim(), ignoreCase = true)
                val saved = savedWords.contains(WordNormalizer.normalize(token.cleanPt))
                Text(
                    text = token.display,
                    modifier = Modifier.clickable {
                        onWordClick(
                            SubtitleWordToken(
                                cleanPt = token.cleanPt,
                                start = token.start,
                                end = token.end,
                            ),
                        )
                    },
                    style = textStyle.copy(
                        textDecoration = if (saved) TextDecoration.Underline else TextDecoration.None,
                    ),
                    color = when {
                        saved -> PpAccent
                        highlighted -> PpAccent
                        else -> PpText
                    },
                    fontWeight = if (highlighted || saved) FontWeight.SemiBold else FontWeight.Normal,
                )
            } else {
                Text(
                    text = token.display,
                    style = textStyle,
                    color = PpText,
                )
            }
        }
    }
}

private data class SubtitleToken(
    val display: String,
    val isWord: Boolean,
    val cleanPt: String = "",
    val start: Int = 0,
    val end: Int = 0,
)

data class SubtitleWordToken(
    val cleanPt: String,
    val start: Int,
    val end: Int,
)

private fun tokenizeSubtitleText(text: String): List<SubtitleToken> {
    val regex = Regex("""(\S+|\s+)""")
    return regex.findAll(text).map { match ->
        val raw = match.value
        val start = match.range.first
        val end = match.range.last + 1
        if (raw.isBlank()) {
            SubtitleToken(display = raw, isWord = false, start = start, end = end)
        } else {
            val clean = raw
                .trim(',', '.', '!', '?', ';', ':', '"', '\'', ')', '»', '…', '—')
                .trim('(', '«', '"', '\'')
            SubtitleToken(
                display = raw,
                isWord = clean.any { it.isLetter() },
                cleanPt = clean,
                start = start,
                end = end,
            )
        }
    }.toList()
}

@Composable
private fun YouTubePlayerSection(
    videoId: String,
    initialSeekSec: Float,
    pendingSeekSec: Float,
    onSeekApplied: () -> Unit,
    onCurrentSec: (Float) -> Unit,
    compact: Boolean,
) {
    val context = LocalContext.current
    var playerError by remember(videoId) { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        key(videoId) {
            YouTubePlayer(
                videoId = videoId,
                initialSeekSec = initialSeekSec,
                pendingSeekSec = pendingSeekSec,
                onSeekApplied = onSeekApplied,
                onCurrentSec = onCurrentSec,
                onError = { playerError = it },
                onPlaybackStarted = { playerError = null },
            )
        }

        if (playerError != null && !compact) {
            MutedText("Встроенный плеер: $playerError")
        }

        if (!compact) {
            Button(
                onClick = {
                    val webUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
                    val youtubeIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                        setPackage("com.google.android.youtube")
                    }
                    try {
                        context.startActivity(youtubeIntent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PpSurfaceInput),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Открыть в YouTube", color = PpText)
            }
        }
    }
}

@Composable
private fun YouTubePlayer(
    videoId: String,
    initialSeekSec: Float,
    pendingSeekSec: Float,
    onSeekApplied: () -> Unit,
    onCurrentSec: (Float) -> Unit,
    onError: (String) -> Unit,
    onPlaybackStarted: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var playerRef by remember { mutableStateOf<YouTubePlayer?>(null) }
    val origin = remember { "https://${context.packageName}" }
    val resumeSec = initialSeekSec.coerceAtLeast(0f)

    LaunchedEffect(pendingSeekSec) {
        if (pendingSeekSec >= 0f) {
            playerRef?.seekTo(pendingSeekSec)
            onSeekApplied()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurface),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)

                    val options = IFramePlayerOptions.Builder()
                        .controls(1)
                        .fullscreen(1)
                        .rel(0)
                        .origin(origin)
                        .build()

                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                playerRef = youTubePlayer
                                youTubePlayer.cueVideo(videoId, resumeSec)
                            }

                            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                                onCurrentSec(second)
                            }

                            override fun onStateChange(
                                youTubePlayer: YouTubePlayer,
                                state: PlayerConstants.PlayerState,
                            ) {
                                if (state == PlayerConstants.PlayerState.PLAYING) {
                                    onPlaybackStarted()
                                }
                            }

                            override fun onError(
                                youTubePlayer: YouTubePlayer,
                                error: PlayerConstants.PlayerError,
                            ) {
                                val message = when (error) {
                                    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
                                        "видео нельзя смотреть во встроенном плеере"
                                    PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
                                        "видео не найдено"
                                    else -> "ошибка $error"
                                }
                                onError(message)
                            }
                        },
                        options,
                    )

                    post {
                        (getChildAt(0) as? WebView)?.settings?.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                        }
                    }
                }
            },
        )
    }

    DisposableEffect(videoId) {
        onDispose { playerRef = null }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PpAccent,
    unfocusedBorderColor = PpBorder,
    focusedContainerColor = PpSurfaceInput,
    unfocusedContainerColor = PpSurfaceInput,
    cursorColor = PpAccent,
    focusedTextColor = PpText,
    unfocusedTextColor = PpText,
)

@Composable
private fun highlightQuery(text: String, query: String): AnnotatedString {
    val accent = PpAccent
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = q.lowercase()
    val start = lowerText.indexOf(lowerQuery)
    if (start < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) {
            append(text.substring(start, start + q.length))
        }
        append(text.substring(start + q.length))
    }
}

private fun formatClock(sec: Float): String {
    val total = sec.toInt().coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
