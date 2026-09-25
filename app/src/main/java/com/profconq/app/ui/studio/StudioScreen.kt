package com.profconq.app.ui.studio

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.R
import com.profconq.app.data.model.Collection
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.studio.StudioCollection
import com.profconq.app.studio.StudioPrefs
import com.profconq.app.ProfconqApplication
import com.profconq.app.studio.StudioStore
import com.profconq.app.studio.StudioSyncService
import com.profconq.app.studio.StudioTtsPlayer
import com.profconq.app.studio.StudioVoice
import com.profconq.app.studio.StudioWordTags
import com.profconq.app.studio.studioIsPhraseTag
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.i18n.UiStrings
import com.profconq.app.ui.components.PortChip
import com.profconq.app.ui.components.PortSegmentedControl
import com.profconq.app.ui.components.PortTextButton
import com.profconq.app.ui.components.PortTextEmphasis
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.study.PracticeSessionColors
import com.profconq.app.ui.theme.PpBg
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpNeonCyan
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.theme.rememberAccentGradientBrush
import com.profconq.app.ui.theme.rememberGlassBorderBrush
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun StudioScreen(
    repository: ProfconqRepository,
    store: StudioStore,
    syncService: StudioSyncService,
    tts: StudioTtsPlayer,
    deckSetId: String,
    deckTitle: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: StudioViewModel = viewModel(
        key = "studio_main",
        factory = StudioViewModelFactory(
            repository,
            store,
            syncService,
            tts,
            (LocalContext.current.applicationContext as ProfconqApplication).studyIntensityStore,
        ),
    ),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalUiStrings.current
    val context = LocalContext.current
    var newTitle by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<StudioCollection?>(null) }
    var playerFullscreen by remember { mutableStateOf(false) }

    val notifyPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) { viewModel.seedDemoIfNeeded(context) }
    LaunchedEffect(deckSetId) { viewModel.reloadLocal() }
    LaunchedEffect(state.playing, state.paused) {
        if ((state.playing || state.paused) && android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) notifyPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(state.sessionOpen) {
        if (!state.sessionOpen) playerFullscreen = false
    }
    BackHandler(enabled = playerFullscreen || state.editorOpen || !state.setupCollapsed || onBack != null) {
        when {
            playerFullscreen -> playerFullscreen = false
            state.editorOpen -> viewModel.closeEditor()
            !state.setupCollapsed -> viewModel.closeSetup()
            else -> onBack?.invoke()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PortLayout.Gutter),
            verticalArrangement = Arrangement.spacedBy(PortLayout.HeaderToContent),
        ) {
            TabScreenHeader(
                title = deckTitle.ifBlank { strings.studioTitle },
                subtitle = if (state.sessionOpen) {
                    "${strings.tabStudio} · ${state.index + 1} / ${state.queue.size}"
                } else {
                    state.collectionTitle.ifBlank { strings.studioSubtitle }
                },
                onBack = onBack,
            )
            when {
                state.isSyncing -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PpNeonGreen,
                    )
                    Text(strings.studioSyncing, color = PpTextMuted, fontSize = 12.sp)
                }
                !state.syncMessage.isNullOrBlank() -> Text(
                    state.syncMessage.orEmpty(),
                    color = PpTextMuted,
                    fontSize = 12.sp,
                )
            }
            val renderedCollections = state.collections.filter { it.render?.hash?.isNotBlank() == true }
            StudioRendersShelf(
                collections = renderedCollections,
                activeId = state.activeId,
                playing = state.playing && !state.paused,
                sessionOpen = state.sessionOpen,
                strings = strings,
                onSelect = { id ->
                    if (state.activeId == id && state.sessionOpen) {
                        viewModel.togglePlayPause()
                    } else {
                        viewModel.startSession(id)
                    }
                },
                onPlay = { id ->
                    if (state.activeId == id && state.sessionOpen) {
                        viewModel.togglePlayPause()
                    } else {
                        viewModel.startSession(id)
                    }
                },
                onDelete = viewModel::deleteCollection,
            )
            StudioCollectionsCard(
                collections = state.collections,
                activeId = state.activeId,
                playingId = if (state.sessionOpen && state.playing && !state.paused) state.activeId else null,
                strings = strings,
                onSelect = viewModel::editCollection,
                onPlay = { id ->
                    if (state.activeId == id && state.sessionOpen) {
                        viewModel.togglePlayPause()
                    } else {
                        viewModel.startSession(id)
                    }
                },
                onCreate = { newTitle = "" },
                onRename = { renameTarget = it },
                onDuplicate = viewModel::duplicateCollection,
                onDelete = viewModel::deleteCollection,
            )
            AnimatedVisibility(
                visible = !state.setupCollapsed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudioCardBox {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = listOf(
                                    strings.studioSetupTitle,
                                    state.collectionTitle.trim().takeIf { it.isNotEmpty() },
                                ).filterNotNull().joinToString(" · "),
                                color = PracticeSessionColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = viewModel::closeSetup) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = strings.cancel,
                                    tint = PracticeSessionColors.TextMuted,
                                )
                            }
                        }
                    }
            StudioSettingsCard(
                prefs = state.prefs,
                strings = strings,
                onDir = viewModel::setDir,
                onVoice = viewModel::setVoice,
                onToggleNum = viewModel::toggleSpeakNum,
                onToggleWord = viewModel::toggleSpeakWord,
                onToggleTr = viewModel::toggleSpeakTr,
                onToggleEx = viewModel::toggleSpeakEx,
                onToggleExRu = viewModel::toggleSpeakExRu,
                onStageSpeed = viewModel::setStageSpeed,
                onStagePause = viewModel::setStagePause,
            )
            StudioWordsCard(
                cards = state.activeCards,
                strings = strings,
                shuffle = state.prefs.shuffle,
                playReady = viewModel.isPlayReady(),
                renderStale = viewModel.isRenderStale(),
                isRendering = state.isRendering,
                renderDone = state.renderDone,
                renderTotal = state.renderTotal,
                renderMessage = state.renderMessage,
                renderVoiceMismatch = state.collections.find { it.id == state.activeId }?.render?.voice
                    ?.takeIf { it.isNotBlank() && it != state.prefs.voice } != null,
                canStart = state.activeCards.isNotEmpty() && !state.sessionOpen && viewModel.isPlayReady() && !state.isRendering,
                editWords = true,
                onToggleShuffle = viewModel::toggleShuffle,
                onStart = { viewModel.startSession() },
                onRender = viewModel::renderCollection,
                onAdd = viewModel::openPicker,
                onRemove = viewModel::removeWord,
            )
                }
            }
            if (state.sessionOpen) {
                StudioPlayerCard(
                    state = state,
                    strings = strings,
                    fullscreen = playerFullscreen,
                    onToggleFullscreen = { playerFullscreen = !playerFullscreen },
                    onTogglePlay = viewModel::togglePlayPause,
                    onPrev = viewModel::prevCard,
                    onNext = viewModel::nextCard,
                    onRestart = viewModel::restartCard,
                    onJump = viewModel::jumpTo,
                    onToggleDir = viewModel::toggleDir,
                    onToggleLoop = viewModel::toggleLoop,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onEdit = viewModel::openEditor,
                    onTag = viewModel::setCurrentTag,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (playerFullscreen && state.sessionOpen) {
        Dialog(
            onDismissRequest = { playerFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PpBg)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = PortLayout.Gutter, vertical = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                StudioPlayerCard(
                    state = state,
                    strings = strings,
                    fullscreen = true,
                    onToggleFullscreen = { playerFullscreen = false },
                    onTogglePlay = viewModel::togglePlayPause,
                    onPrev = viewModel::prevCard,
                    onNext = viewModel::nextCard,
                    onRestart = viewModel::restartCard,
                    onJump = viewModel::jumpTo,
                    onToggleDir = viewModel::toggleDir,
                    onToggleLoop = viewModel::toggleLoop,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onEdit = viewModel::openEditor,
                    onTag = viewModel::setCurrentTag,
                )
            }
        }
    }

    if (state.editorOpen && state.current != null) {
        StudioCardEditorDialog(
            card = state.current!!,
            strings = strings,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::saveEditedCard,
        )
    }
    if (state.pickerOpen) {
        StudioPickerDialog(
            folders = state.dictionaryFolders,
            already = state.activeCards.map { it.id }.toSet(),
            strings = strings,
            onDismiss = viewModel::closePicker,
            onAdd = { ids ->
                viewModel.addWords(ids)
                viewModel.closePicker()
            },
        )
    }
    newTitle?.let { draft ->
        StudioNameDialog(
            title = strings.studioNewCollection,
            value = draft,
            confirm = strings.studyConfirmCreate,
            onValue = { newTitle = it },
            onDismiss = { newTitle = null },
            onConfirm = {
                viewModel.createCollection(draft.ifBlank { strings.studioTitle })
                newTitle = null
            },
        )
    }
    renameTarget?.let { col ->
        var value by remember(col.id) { mutableStateOf(col.title) }
        StudioNameDialog(
            title = strings.studioRename,
            value = value,
            confirm = strings.studioSave,
            onValue = { value = it },
            onDismiss = { renameTarget = null },
            onConfirm = {
                viewModel.renameCollection(col.id, value)
                renameTarget = null
            },
        )
    }
}

@Composable
private fun StudioSettingsCard(
    prefs: StudioPrefs,
    strings: UiStrings,
    onDir: (Boolean) -> Unit,
    onVoice: (String) -> Unit,
    onToggleNum: () -> Unit,
    onToggleWord: () -> Unit,
    onToggleTr: () -> Unit,
    onToggleEx: () -> Unit,
    onToggleExRu: () -> Unit,
    onStageSpeed: (String, Float) -> Unit,
    onStagePause: (String, Float) -> Unit,
) {
    val voices = StudioVoice.entries
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = StudioCardColors.Cyan.copy(alpha = 0.22f),
                spotColor = StudioCardColors.Cyan.copy(alpha = 0.16f),
            )
            .background(PracticeSessionColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, StudioCardColors.Cyan.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(StudioCardColors.Cyan.copy(alpha = 0.12f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.42f,
                    ),
                )
            }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StudioMixerLabel(strings.studioVoice)
        StudioSegBar(
            items = voices.map { it.label },
            selectedIndex = voices.indexOfFirst { it.key == prefs.voice }.coerceAtLeast(0),
            onSelect = { onVoice(voices[it].key) },
        )
        StudioMixerLabel(strings.studioDirection)
        StudioSegBar(
            items = listOf(
                strings.studioDirLabel(prefs.sourceLang, prefs.targetLang),
                strings.studioDirLabel(prefs.targetLang, prefs.sourceLang),
            ),
            selectedIndex = if (prefs.dirPtToRu) 0 else 1,
            onSelect = { onDir(it == 0) },
        )
        StudioSpeakTable(
            prefs = prefs,
            strings = strings,
            onToggleNum = onToggleNum,
            onToggleWord = onToggleWord,
            onToggleTr = onToggleTr,
            onToggleEx = onToggleEx,
            onToggleExRu = onToggleExRu,
            onStageSpeed = onStageSpeed,
            onStagePause = onStagePause,
        )
    }
}

@Composable
private fun StudioSpeakTable(
    prefs: StudioPrefs,
    strings: UiStrings,
    onToggleNum: () -> Unit,
    onToggleWord: () -> Unit,
    onToggleTr: () -> Unit,
    onToggleEx: () -> Unit,
    onToggleExRu: () -> Unit,
    onStageSpeed: (String, Float) -> Unit,
    onStagePause: (String, Float) -> Unit,
) {
    val rows = listOf(
        StudioSpeakRow(
            kind = "num",
            label = strings.studioSpeakNumShort,
            speak = prefs.speakNum,
            speed = prefs.speedNum,
            pause = prefs.pauseNumSec,
            onToggle = onToggleNum,
        ),
        StudioSpeakRow(
            kind = "word",
            label = strings.studioSpeakWordLabel(prefs.sourceLang),
            speak = prefs.speakWord,
            speed = prefs.speedWord,
            pause = prefs.pauseWordSec,
            onToggle = onToggleWord,
        ),
        StudioSpeakRow(
            kind = "tr",
            label = strings.studioSpeakTrLabel(prefs.targetLang),
            speak = prefs.speakTr,
            speed = prefs.speedTr,
            pause = prefs.pauseTrSec,
            onToggle = onToggleTr,
        ),
        StudioSpeakRow(
            kind = "ex",
            label = strings.studioSpeakExLabel(prefs.sourceLang),
            speak = prefs.speakEx,
            speed = prefs.speedEx,
            pause = prefs.pauseExSec,
            onToggle = onToggleEx,
        ),
        StudioSpeakRow(
            kind = "exRu",
            label = strings.studioSpeakExTrLabel(prefs.targetLang),
            speak = prefs.speakExRu,
            speed = prefs.speedExRu,
            pause = prefs.pauseExRuSec,
            onToggle = onToggleExRu,
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.studioSpeakWhat,
                color = StudioCardColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = strings.studioColSpeak.uppercase(),
                color = StudioCardColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp),
            )
            Text(
                text = strings.studioSpeed.uppercase(),
                color = StudioCardColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(92.dp),
            )
            Text(
                text = strings.studioColPause.uppercase(),
                color = StudioCardColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(92.dp),
            )
        }
        rows.forEach { row ->
            StudioSpeakTableRow(
                label = row.label,
                speak = row.speak,
                speed = row.speed,
                pause = row.pause,
                onToggle = row.onToggle,
                onSpeed = { onStageSpeed(row.kind, it) },
                onPause = { onStagePause(row.kind, it) },
            )
        }
        StudioSpeakTableRow(
            label = strings.studioPauseCardsShort,
            speak = null,
            speed = null,
            pause = prefs.pauseBetweenCardsSec,
            onToggle = null,
            onSpeed = null,
            onPause = { onStagePause("cards", it) },
        )
    }
}

private data class StudioSpeakRow(
    val kind: String,
    val label: String,
    val speak: Boolean,
    val speed: Float,
    val pause: Float,
    val onToggle: () -> Unit,
)

@Composable
private fun StudioSpeakTableRow(
    label: String,
    speak: Boolean?,
    speed: Float?,
    pause: Float,
    onToggle: (() -> Unit)?,
    onSpeed: ((Float) -> Unit)?,
    onPause: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(PracticeSessionColors.BgElev.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = PracticeSessionColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (speak != null && onToggle != null) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Checkbox(
                        checked = speak,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = StudioCardColors.Cyan,
                            uncheckedColor = StudioCardColors.Muted,
                            checkmarkColor = PracticeSessionColors.OnAccent,
                        ),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Box(
            modifier = Modifier.width(92.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (speed != null && onSpeed != null) {
                StudioTinyStepper(
                    text = formatStudioSpeed(speed),
                    onMinus = { onSpeed(prevStudioSpeed(speed)) },
                    onPlus = { onSpeed(nextStudioSpeed(speed)) },
                )
            }
        }
        Box(
            modifier = Modifier.width(92.dp),
            contentAlignment = Alignment.Center,
        ) {
            StudioTinyStepper(
                text = formatStudioPause(pause),
                onMinus = { onPause((pause - 0.5f).coerceAtLeast(0f)) },
                onPlus = { onPause((pause + 0.5f).coerceAtMost(5f)) },
            )
        }
    }
}

private val StudioSpeedSteps = listOf(0.75f, 1f, 1.25f, 1.5f)

private fun nextStudioSpeed(current: Float): Float {
    val i = StudioSpeedSteps.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }.let { if (it < 0) 1 else it }
    return StudioSpeedSteps[(i + 1).coerceAtMost(StudioSpeedSteps.lastIndex)]
}

private fun prevStudioSpeed(current: Float): Float {
    val i = StudioSpeedSteps.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }.let { if (it < 0) 1 else it }
    return StudioSpeedSteps[(i - 1).coerceAtLeast(0)]
}

private fun formatStudioSpeed(speed: Float): String = when {
    kotlin.math.abs(speed - 1f) < 0.01f -> "1×"
    kotlin.math.abs(speed - 0.75f) < 0.01f -> "0.75×"
    kotlin.math.abs(speed - 1.25f) < 0.01f -> "1.25×"
    kotlin.math.abs(speed - 1.5f) < 0.01f -> "1.5×"
    else -> "${speed}×"
}

private fun formatStudioPause(sec: Float): String {
    val shown = if (sec == sec.toLong().toFloat()) "${sec.toInt()}" else "$sec".trimEnd('0').trimEnd('.')
    return "${shown}s"
}

@Composable
private fun StudioTinyStepper(
    text: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StudioTinyStepButton("−", onMinus)
        Text(
            text = text,
            color = StudioCardColors.Cyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp),
        )
        StudioTinyStepButton("+", onPlus)
    }
}

@Composable
private fun StudioTinyStepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StudioCardColors.Cyan.copy(alpha = 0.12f))
            .border(1.dp, StudioCardColors.Cyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = StudioCardColors.Cyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StudioRendersShelf(
    collections: List<StudioCollection>,
    activeId: String?,
    playing: Boolean,
    sessionOpen: Boolean,
    strings: UiStrings,
    onSelect: (String) -> Unit,
    onPlay: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var armedId by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            strings.studioRenders,
            color = PracticeSessionColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        if (collections.isEmpty()) {
            Text(strings.studioNoRenders, color = PracticeSessionColors.TextMuted, fontSize = 13.sp)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                collections.forEach { col ->
                val selected = col.id == activeId
                val isPlaying = selected && sessionOpen && playing
                StudioRenderThumb(
                    title = col.title,
                    subtitle = strings.studyWordsCount(col.resolvedCount.coerceAtLeast(col.webWordIds.size)),
                    selected = selected,
                    playing = isPlaying,
                    armed = armedId == col.id,
                    deleteLabel = strings.delete,
                    onSelect = {
                        armedId = null
                        onSelect(col.id)
                    },
                    onPlay = {
                        if (armedId == col.id) {
                            armedId = null
                        } else {
                            armedId = null
                            onPlay(col.id)
                        }
                    },
                    onArmDelete = { armedId = col.id },
                    onDelete = {
                        armedId = null
                        onDelete(col.id)
                    },
                )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StudioRenderThumb(
    title: String,
    subtitle: String,
    selected: Boolean,
    playing: Boolean,
    armed: Boolean,
    deleteLabel: String,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onArmDelete: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier.width(112.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    if (selected || playing || armed) 10.dp else 3.dp,
                    shape,
                    ambientColor = if (armed) {
                        PracticeSessionColors.Danger.copy(alpha = 0.4f)
                    } else {
                        StudioCardColors.Cyan.copy(alpha = if (playing) 0.45f else 0.18f)
                    },
                    spotColor = if (armed) {
                        PracticeSessionColors.Danger.copy(alpha = 0.35f)
                    } else {
                        PpNeonGreen.copy(alpha = if (playing) 0.4f else 0.16f)
                    },
                )
                .clip(shape)
                .background(Color(0xFF0A1224))
                .border(
                    width = if (playing || armed) 1.5.dp else 1.dp,
                    brush = Brush.horizontalGradient(
                        if (armed) {
                            listOf(
                                PracticeSessionColors.Danger.copy(alpha = 0.9f),
                                PracticeSessionColors.Danger.copy(alpha = 0.45f),
                            )
                        } else {
                            listOf(
                                StudioCardColors.Cyan.copy(alpha = if (selected) 0.75f else 0.35f),
                                PpNeonGreen.copy(alpha = if (playing) 0.7f else 0.28f),
                            )
                        },
                    ),
                    shape = shape,
                )
                .combinedClickable(
                    onClick = onPlay,
                    onLongClick = onArmDelete,
                )
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color(0x3300D2FF), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.55f,
                        ),
                    )
                },
        ) {
            Image(
                painter = painterResource(R.drawable.studio_waveform),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .background(Color(0xE6050B18), CircleShape)
                    .border(1.dp, StudioCardColors.Cyan.copy(alpha = 0.55f), CircleShape)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = title,
                    tint = StudioCardColors.Cyan,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (armed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC050B18))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = deleteLabel,
                            tint = PracticeSessionColors.Danger,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = deleteLabel,
                            color = PracticeSessionColors.Danger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        Text(
            text = title,
            color = if (selected) StudioCardColors.Cyan else PracticeSessionColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onSelect),
        )
        Text(
            text = subtitle,
            color = PracticeSessionColors.TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StudioCollectionsCard(
    collections: List<StudioCollection>,
    activeId: String?,
    playingId: String?,
    strings: UiStrings,
    onSelect: (String) -> Unit,
    onPlay: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (StudioCollection) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    StudioCardBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(strings.studioCollections, color = PracticeSessionColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            PortTextButton(
                text = strings.studioNew,
                onClick = onCreate,
                leading = {
                    Icon(Icons.Default.Add, null, tint = PracticeSessionColors.Accent, modifier = Modifier.size(16.dp))
                },
            )
        }
        if (collections.isEmpty()) {
            Text(strings.studioEmpty, color = PracticeSessionColors.TextMuted, fontSize = 13.sp)
        } else {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
            collections.forEach { col ->
                val selected = col.id == activeId
                val rendered = col.render?.hash?.isNotBlank() == true
                val playing = col.id == playingId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            if (selected) PracticeSessionColors.AccentSoft else Color.Transparent,
                            RoundedCornerShape(10.dp),
                        )
                        .border(
                            1.dp,
                            if (selected) PracticeSessionColors.Accent else PracticeSessionColors.Border,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { onSelect(col.id) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (rendered) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0A1224))
                                .border(1.dp, StudioCardColors.Cyan.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .clickable(onClick = { onPlay(col.id) }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.studio_waveform),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                            )
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = strings.studioPlay,
                                tint = StudioCardColors.Cyan,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                    Text(
                        text = col.title,
                        color = PracticeSessionColors.TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = strings.studyWordsCount(col.resolvedCount.coerceAtLeast(col.webWordIds.size)),
                        color = PracticeSessionColors.TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        IconButton(
                            onClick = { onRename(col) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Default.Edit, null, tint = PracticeSessionColors.TextFaint, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onDuplicate(col.id) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = PracticeSessionColors.TextFaint, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onDelete(col.id) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Default.Delete, null, tint = PracticeSessionColors.Danger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun StudioWordsCard(
    cards: List<WordCard>,
    strings: UiStrings,
    shuffle: Boolean,
    playReady: Boolean,
    renderStale: Boolean,
    isRendering: Boolean,
    renderDone: Int,
    renderTotal: Int,
    renderMessage: String?,
    renderVoiceMismatch: Boolean,
    canStart: Boolean,
    editWords: Boolean,
    onToggleShuffle: () -> Unit,
    onStart: () -> Unit,
    onRender: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    StudioCardBox {
        Text(strings.studioSelectWords, color = PracticeSessionColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(
            if (cards.isEmpty()) strings.studioNoWords else strings.studyWordsCount(cards.size),
            color = PracticeSessionColors.TextMuted,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cards.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEachIndexed { colIndex, card ->
                        StudioWordCell(
                            number = rowIndex * 2 + colIndex + 1,
                            label = card.pt.ifBlank { card.ru },
                            onRemove = if (editWords) {
                                { onRemove(card.id) }
                            } else {
                                null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        val statusText = when {
            cards.isEmpty() -> strings.studioNoWords
            isRendering -> strings.studioRenderProgress(renderDone, renderTotal)
            playReady -> strings.studioRenderReady(cards.size)
            renderMessage == "fail" -> strings.studioRenderFail
            renderMessage == "cancel" -> strings.studioRenderCancelled
            renderVoiceMismatch -> strings.studioRenderStaleVoice
            renderStale -> strings.studioRenderStale
            renderMessage == "need" -> strings.studioPlayNeedsRender
            else -> strings.studioRenderNeeded
        }
        Text(
            text = statusText,
            color = when {
                playReady -> PracticeSessionColors.Accent
                renderMessage == "fail" -> PracticeSessionColors.Danger
                else -> PracticeSessionColors.TextMuted
            },
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (isRendering && renderTotal > 0) {
            LinearProgressIndicator(
                progress = { (renderDone.toFloat() / renderTotal.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
                color = PracticeSessionColors.Accent,
                trackColor = PracticeSessionColors.Border,
            )
        }
        PortTextButton(
            text = when {
                isRendering -> strings.studioRenderCancelLabel(renderDone, renderTotal)
                playReady || renderStale || renderVoiceMismatch -> strings.studioReRenderCollection
                else -> strings.studioRenderCollection
            },
            onClick = onRender,
            enabled = cards.isNotEmpty(),
            emphasis = if (isRendering || !playReady) PortTextEmphasis.Accent else PortTextEmphasis.Muted,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editWords) {
                PortTextButton(
                    text = strings.studioAddMore,
                    onClick = onAdd,
                    leading = {
                        Icon(Icons.Default.Add, null, tint = PracticeSessionColors.Accent, modifier = Modifier.size(16.dp))
                    },
                )
            }
            PortTextButton(
                text = strings.studioShuffle,
                onClick = onToggleShuffle,
                emphasis = if (shuffle) PortTextEmphasis.Accent else PortTextEmphasis.Muted,
                leading = {
                    Icon(
                        Icons.Default.Shuffle,
                        null,
                        tint = if (shuffle) PracticeSessionColors.Accent else PracticeSessionColors.TextFaint,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            PortTextButton(
                text = strings.studioStart,
                onClick = onStart,
                enabled = canStart,
                leading = {
                    Icon(Icons.Default.PlayArrow, null, tint = PracticeSessionColors.Accent, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}

@Composable
private fun StudioWordCell(
    number: Int,
    label: String,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .background(PracticeSessionColors.BgElev, RoundedCornerShape(10.dp))
            .border(1.dp, PracticeSessionColors.Border, RoundedCornerShape(10.dp))
            .padding(start = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = number.toString(),
            color = PracticeSessionColors.TextFaint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(18.dp),
        )
        Text(
            text = label,
            color = PracticeSessionColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = PracticeSessionColors.TextFaint,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun StudioPlayerCard(
    state: StudioUiState,
    strings: UiStrings,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onJump: (Int) -> Unit,
    onToggleDir: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleShuffle: () -> Unit,
    onEdit: () -> Unit,
    onTag: (String) -> Unit,
) {
    val card = state.current
    val prefs = state.prefs
    val status = when {
        state.status == "done" -> strings.studioDone
        state.status == "empty" -> strings.studioNoWords
        state.status.startsWith("next:") -> strings.studioNextIn(state.status.removePrefix("next:"))
        else -> strings.studioCardProgress(state.index + 1, state.queue.size)
    }
    StudioCardBox {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = status,
                color = PracticeSessionColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (fullscreen) strings.studioFullscreenExit else strings.studioFullscreen,
                    tint = StudioCardColors.Cyan,
                )
            }
        }
        StudioFilmstrip(
            queue = state.queue,
            index = state.index,
            dirPtToRu = prefs.dirPtToRu,
            onJump = onJump,
        )
        Spacer(modifier = Modifier.height(8.dp))
        StudioRadioScale(
            elapsedMs = state.cardElapsedMs,
            durationMs = state.cardDurationMs,
        )
        StudioPlayFace(
            card = card,
            reveal = state.reveal,
            dirPtToRu = prefs.dirPtToRu,
            sourceLang = prefs.sourceLang,
            targetLang = prefs.targetLang,
            strings = strings,
            onToggleDir = onToggleDir,
            onTag = onTag,
            onNext = onNext,
            onPrev = onPrev,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioCtrlButton(
                onClick = onToggleShuffle,
                on = prefs.shuffle,
            ) {
                Icon(Icons.Default.Shuffle, strings.studioShuffle, tint = it, modifier = Modifier.size(StudioCtrlIconSize))
            }
            StudioCtrlButton(onClick = onPrev) {
                Icon(Icons.Default.SkipPrevious, null, tint = it, modifier = Modifier.size(StudioCtrlIconSize))
            }
            StudioCtrlButton(onClick = onRestart) {
                Icon(Icons.Default.Replay, null, tint = it, modifier = Modifier.size(StudioCtrlIconSize))
            }
            StudioPlayButton(
                playing = state.playing && !state.paused,
                onClick = onTogglePlay,
            )
            StudioCtrlButton(onClick = onEdit, enabled = card != null) {
                Icon(Icons.Default.Edit, strings.studioEditCard, tint = it, modifier = Modifier.size(StudioCtrlIconSize))
            }
            StudioCtrlButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, null, tint = it, modifier = Modifier.size(StudioCtrlIconSize))
            }
            StudioCtrlButton(onClick = onToggleLoop, on = prefs.loop) {
                Icon(Icons.Default.Repeat, null, tint = it, modifier = Modifier.size(StudioCtrlIconSize))
            }
        }
    }
}

@Composable
private fun StudioPickerDialog(
    folders: List<Collection>,
    already: Set<String>,
    strings: UiStrings,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val openFolder = folders.find { it.id == openFolderId }
    val q = query.trim()
    fun WordCard.matchesQuery(): Boolean {
        if (q.isEmpty()) return true
        val hay = "$pt $ru ${example.orEmpty()} ${exampleTranslation.orEmpty()}"
        return hay.contains(q, ignoreCase = true)
    }
    fun Collection.availableIds(): List<String> = cards.map { it.id }.filter { it !in already }
    fun toggleIds(ids: List<String>) {
        if (ids.isEmpty()) return
        selected = if (ids.all { it in selected }) selected - ids.toSet() else selected + ids
    }
    val visibleFolders = if (q.isEmpty()) folders else folders.filter { folder ->
        folder.title.contains(q, ignoreCase = true) || folder.cards.any { it.matchesQuery() }
    }
    val visibleWords = when {
        openFolder != null -> openFolder.cards.filter { it.matchesQuery() }
        q.isNotEmpty() -> folders.flatMap { it.cards }.filter { it.matchesQuery() }
        else -> emptyList()
    }
    val showFolders = openFolder == null && q.isEmpty()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 420.dp, max = 640.dp)
                .background(PracticeSessionColors.Card, RoundedCornerShape(20.dp))
                .border(1.dp, PracticeSessionColors.Border, RoundedCornerShape(20.dp)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = openFolder?.title ?: strings.studioAddFromDict,
                        color = PracticeSessionColors.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = if (openFolder == null) strings.studioPickerSub else strings.studioPickerMarkWords,
                        color = PracticeSessionColors.TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = PracticeSessionColors.TextMuted)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(strings.studioPickerSearch, color = PracticeSessionColors.TextFaint) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = PortLayout.Gutter, vertical = 4.dp),
            )
            if (openFolder != null) {
                val available = openFolder.availableIds()
                val allOn = available.isNotEmpty() && available.all { it in selected }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PortTextButton(
                        text = strings.studioPickerBack,
                        onClick = { openFolderId = null; query = "" },
                        emphasis = PortTextEmphasis.Muted,
                        leading = {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PracticeSessionColors.TextMuted, modifier = Modifier.size(16.dp))
                        },
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    PortTextButton(
                        text = if (allOn) strings.studioPickerDeselectAll else strings.studioPickerSelectAll,
                        onClick = { toggleIds(available) },
                        enabled = available.isNotEmpty(),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showFolders) {
                    if (visibleFolders.isEmpty()) {
                        Text(strings.studioEmptyDict, color = PracticeSessionColors.TextMuted, modifier = Modifier.padding(24.dp))
                    } else {
                        visibleFolders.forEach { folder ->
                            val available = folder.availableIds()
                            val allOn = available.isNotEmpty() && available.all { it in selected }
                            StudioPickerFolderRow(
                                title = folder.title,
                                countLabel = strings.studyWordsCount(folder.cards.size),
                                addLabel = when {
                                    available.isEmpty() -> strings.studioPickerFolderDone
                                    allOn -> strings.studioPickerFolderPicked
                                    else -> strings.studioPickerAddFolder
                                },
                                selected = allOn,
                                addEnabled = available.isNotEmpty(),
                                onOpen = { openFolderId = folder.id; query = "" },
                                onAddFolder = { toggleIds(available) },
                            )
                        }
                    }
                } else if (visibleWords.isEmpty()) {
                    Text(strings.studioEmptyDict, color = PracticeSessionColors.TextMuted, modifier = Modifier.padding(24.dp))
                } else {
                    visibleWords.forEach { card ->
                        val taken = card.id in already
                        StudioPickerWordRow(
                            card = card,
                            taken = taken,
                            checked = taken || card.id in selected,
                            onToggle = {
                                if (!taken) {
                                    selected = if (card.id in selected) selected - card.id else selected + card.id
                                }
                            },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PortTextButton(
                    text = strings.studioCancel,
                    onClick = onDismiss,
                    emphasis = PortTextEmphasis.Muted,
                )
                PortTextButton(
                    text = strings.studioAddCount(selected.size),
                    onClick = { onAdd(selected.toList()) },
                    enabled = selected.isNotEmpty(),
                )
            }
        }
    }
}

@Composable
private fun StudioPickerFolderRow(
    title: String,
    countLabel: String,
    addLabel: String,
    selected: Boolean,
    addEnabled: Boolean,
    onOpen: () -> Unit,
    onAddFolder: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) StudioCardColors.DirBg else PracticeSessionColors.BgElev,
                RoundedCornerShape(14.dp),
            )
            .border(
                1.dp,
                if (selected) StudioCardColors.Cyan.copy(alpha = 0.55f) else PracticeSessionColors.Border,
                RoundedCornerShape(14.dp),
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(StudioCardColors.DirBg, RoundedCornerShape(12.dp))
                    .border(1.dp, StudioCardColors.DirBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Folder, null, tint = StudioCardColors.Cyan, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = PracticeSessionColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(countLabel, color = PracticeSessionColors.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = PracticeSessionColors.TextFaint, modifier = Modifier.size(18.dp))
        }
        Box(
            modifier = Modifier
                .background(
                    if (selected) StudioCardColors.DirBg else PracticeSessionColors.Card,
                    RoundedCornerShape(999.dp),
                )
                .border(1.dp, if (selected) StudioCardColors.Cyan else PracticeSessionColors.Border, RoundedCornerShape(999.dp))
                .clickable(enabled = addEnabled, onClick = onAddFolder)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                addLabel,
                color = if (addEnabled) StudioCardColors.Cyan else PracticeSessionColors.TextFaint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StudioPickerWordRow(
    card: WordCard,
    taken: Boolean,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PracticeSessionColors.BgElev, RoundedCornerShape(12.dp))
            .clickable(enabled = !taken, onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { if (!taken) onToggle() }, enabled = !taken)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.pt.ifBlank { card.example.orEmpty() }.ifBlank { "—" },
                color = if (taken) PracticeSessionColors.TextFaint else PracticeSessionColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            val ru = card.ru.ifBlank { card.exampleTranslation.orEmpty() }
            if (ru.isNotBlank()) {
                Text(ru, color = PracticeSessionColors.TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StudioNameDialog(
    title: String,
    value: String,
    confirm: String,
    onValue: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val strings = LocalUiStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PracticeSessionColors.Card,
        title = { Text(title, color = PracticeSessionColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { PortTextButton(text = confirm, onClick = onConfirm) },
        dismissButton = { PortTextButton(text = strings.studioCancel, onClick = onDismiss, emphasis = PortTextEmphasis.Muted) },
    )
}

private val StudioCtrlSize = 42.dp
private val StudioCtrlIconSize = 20.dp

@Composable
private fun StudioCtrlButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    on: Boolean = false,
    icon: @Composable (Color) -> Unit,
) {
    val tint = when {
        !enabled -> PracticeSessionColors.TextFaint
        on -> PpBrandNavy
        else -> PracticeSessionColors.TextPrimary
    }
    StudioPressCircle(
        onClick = onClick,
        enabled = enabled,
        active = on,
    ) {
        icon(tint)
    }
}

@Composable
private fun StudioPlayButton(
    playing: Boolean,
    onClick: () -> Unit,
) {
    StudioPressCircle(
        onClick = onClick,
        prominent = true,
        active = playing,
    ) {
        Icon(
            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = PpBrandNavy,
            modifier = Modifier
                .size(StudioCtrlIconSize)
                .padding(start = if (playing) 0.dp else 1.dp),
        )
    }
}

@Composable
private fun StudioPressCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    prominent: Boolean = false,
    size: Dp = StudioCtrlSize,
    pressScale: Float = 0.88f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val lit = prominent || active
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) pressScale else 1f,
        animationSpec = spring(stiffness = 640f, dampingRatio = 0.52f),
        label = "studioCirclePress",
    )
    val glow by animateFloatAsState(
        targetValue = when {
            !enabled -> 0f
            pressed -> 0.28f
            lit -> 1f
            else -> 0.18f
        },
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.75f),
        label = "studioCircleGlow",
    )
    val pressOffset = with(LocalDensity.current) { 2.5.dp.toPx() }
    val gradient = rememberAccentGradientBrush()
    val glass = rememberGlassBorderBrush()
    val idle = Brush.verticalGradient(
        listOf(Color(0xFF1A2A44), Color(0xFF10192B)),
    )
    Box(
        modifier = modifier
            .requiredSize(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (enabled && pressed) pressOffset else 0f
            }
            .shadow(
                elevation = if (pressed) 2.dp else 10.dp * glow,
                shape = CircleShape,
                ambientColor = PpNeonCyan.copy(alpha = 0.5f * glow),
                spotColor = PpNeonGreen.copy(alpha = 0.55f * glow),
            )
            .clip(CircleShape)
            .background(if (lit) gradient else idle, CircleShape)
            .border(
                width = 1.dp,
                brush = if (lit) {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), PpNeonCyan.copy(alpha = 0.5f)))
                } else {
                    glass
                },
                shape = CircleShape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = PpNeonCyan),
                onClick = onClick,
            )
            .drawBehind {
                if (!pressed) {
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                            startY = 0f,
                            endY = this.size.height * 0.55f,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f }) {
            content()
        }
    }
}

private fun formatStudioClock(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val min = (totalSec % 3600L) / 60L
    val sec = totalSec % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, min, sec)
    } else {
        "%d:%02d".format(min, sec)
    }
}

@Composable
private fun StudioRadioScale(
    elapsedMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val fraction = if (durationMs > 0L) {
        (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val tick = Color(0xFFD8B48A)
    val tickMajor = Color(0xFFF3D7A8)
    val rail = Color(0xFF3A2A18)
    val needle = StudioCardColors.Cyan
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatStudioClock(elapsedMs),
                color = needle,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatStudioClock(durationMs),
                color = StudioCardColors.Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A0C12))
                .border(1.dp, Color(0xFF2A2216), RoundedCornerShape(8.dp)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pad = 10.dp.toPx()
                val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                val baseY = size.height * 0.62f
                drawLine(
                    color = rail,
                    start = Offset(pad, baseY),
                    end = Offset(size.width - pad, baseY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val marks = 50
                for (i in 0..marks) {
                    val x = pad + usable * (i / marks.toFloat())
                    val major = i % 10 == 0
                    val mid = i % 5 == 0
                    val h = when {
                        major -> 15.dp.toPx()
                        mid -> 10.dp.toPx()
                        else -> 5.dp.toPx()
                    }
                    drawLine(
                        color = if (major) tickMajor else tick.copy(alpha = if (mid) 0.85f else 0.45f),
                        start = Offset(x, baseY),
                        end = Offset(x, baseY - h),
                        strokeWidth = if (major) 1.6.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                val nx = pad + usable * fraction
                val side = 9.dp.toPx()
                val triH = side * sqrt(3f) / 2f
                val half = side / 2f
                val baseBottom = baseY + 1.5.dp.toPx()
                val tipY = baseBottom - triH
                val glow = Path().apply {
                    val g = 1.2.dp.toPx()
                    moveTo(nx, tipY - g)
                    lineTo(nx - half - g, baseBottom + g * 0.4f)
                    lineTo(nx + half + g, baseBottom + g * 0.4f)
                    close()
                }
                drawPath(glow, needle.copy(alpha = 0.22f))
                val marker = Path().apply {
                    moveTo(nx, tipY)
                    lineTo(nx - half, baseBottom)
                    lineTo(nx + half, baseBottom)
                    close()
                }
                drawPath(
                    path = marker,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, needle, Color(0xFF007A99)),
                        startY = tipY,
                        endY = baseBottom,
                    ),
                )
                drawPath(
                    path = marker,
                    color = Color.White.copy(alpha = 0.7f),
                    style = Stroke(width = 0.9.dp.toPx(), join = StrokeJoin.Round),
                )
                val inner = Path().apply {
                    val innerSide = side * 0.42f
                    val innerH = innerSide * sqrt(3f) / 2f
                    val innerHalf = innerSide / 2f
                    val innerBase = baseBottom - 1.1.dp.toPx()
                    moveTo(nx, innerBase - innerH)
                    lineTo(nx - innerHalf, innerBase)
                    lineTo(nx + innerHalf, innerBase)
                    close()
                }
                drawPath(inner, Color.White.copy(alpha = 0.18f))
            }
        }
    }
}

@Composable
private fun StudioFilmstrip(
    queue: List<WordCard>,
    index: Int,
    dirPtToRu: Boolean,
    onJump: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canPage = queue.size > 4
    LaunchedEffect(index, queue.size) {
        if (index in queue.indices) {
            listState.animateScrollToItem(index)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StudioFilmNav(
            enabled = canPage && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0),
            left = true,
            onClick = {
                scope.launch {
                    val target = (listState.firstVisibleItemIndex - 4).coerceAtLeast(0)
                    listState.animateScrollToItem(target)
                }
            },
        )
        StudioTapeWindow(modifier = Modifier.weight(1f).height(84.dp)) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                itemsIndexed(queue, key = { i, card -> "${card.id}-$i" }) { i, item ->
                    StudioFilmThumb(
                        number = i + 1,
                        label = studioFilmLabel(item, dirPtToRu),
                        active = i == index,
                        onClick = { onJump(i) },
                    )
                }
            }
        }
        StudioFilmNav(
            enabled = canPage && listState.canScrollForward,
            left = false,
            onClick = {
                scope.launch {
                    val target = (listState.firstVisibleItemIndex + 4).coerceAtMost((queue.lastIndex).coerceAtLeast(0))
                    listState.animateScrollToItem(target)
                }
            },
        )
    }
}

@Composable
private fun StudioTapeWindow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val window = RoundedCornerShape(14.dp)
    val glass = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .shadow(6.dp, window, ambientColor = Color(0x66000000), spotColor = Color(0x66000000))
            .clip(window)
            .background(Color(0xFF070E1C), window)
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        StudioCardColors.Cyan.copy(alpha = 0.55f),
                        StudioCardColors.Cyan.copy(alpha = 0.22f),
                        PpNeonGreen.copy(alpha = 0.45f),
                    ),
                ),
                shape = window,
            )
            .drawBehind {
                val inset = 4.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF0A1224),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                    cornerRadius = CornerRadius(11.dp.toPx()),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x3300D2FF), Color.Transparent),
                        startY = inset,
                        endY = size.height * 0.42f,
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2f, size.height - inset * 2f),
                    cornerRadius = CornerRadius(11.dp.toPx()),
                )
                val step = 10.dp.toPx()
                val dot = Color.White.copy(alpha = 0.045f)
                var x = inset + 8.dp.toPx()
                while (x < size.width - inset) {
                    var y = inset + 8.dp.toPx()
                    while (y < size.height - inset) {
                        drawCircle(dot, 1.1f, Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }
            .padding(3.dp)
            .clip(glass),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(22.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF070E1C), Color.Transparent),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(22.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF070E1C)),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun StudioFilmNav(enabled: Boolean, left: Boolean, onClick: () -> Unit) {
    StudioPressCircle(
        onClick = onClick,
        enabled = enabled,
        size = StudioCtrlSize,
        pressScale = 0.9f,
    ) {
        Icon(
            imageVector = if (left) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = PracticeSessionColors.TextPrimary,
            modifier = Modifier.size(StudioCtrlIconSize),
        )
    }
}

@Composable
private fun StudioFilmThumb(
    number: Int,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.55f),
        label = "studioThumbPress",
    )
    val pressOffset = with(LocalDensity.current) { 2.dp.toPx() }
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) pressOffset else 0f
            }
            .width(80.dp)
            .height(56.dp)
            .shadow(
                if (active) 8.dp else if (pressed) 2.dp else 3.dp,
                shape,
                ambientColor = if (active) PpNeonCyan.copy(alpha = 0.4f) else Color(0x55000000),
                spotColor = if (active) PpNeonGreen.copy(alpha = 0.35f) else Color(0x55000000),
            )
            .clip(shape)
            .drawBehind { drawStudioNotebook() }
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) StudioCardColors.Cyan else StudioCardColors.Border,
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = PpNeonCyan),
                onClick = onClick,
            )
            .padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = number.toString(),
                color = if (active) StudioCardColors.Cyan else StudioCardColors.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
            )
            Text(
                text = label,
                color = StudioCardColors.Text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun studioFilmLabel(card: WordCard, dirPtToRu: Boolean): String {
    if (studioIsPhraseTag(card.partOfSpeech)) {
        val pt = card.example.orEmpty().ifBlank { card.pt }
        val ru = card.exampleTranslation.orEmpty().ifBlank { card.ru }
        return (if (dirPtToRu) pt else ru).trim().ifBlank { "—" }
    }
    val main = if (dirPtToRu) {
        card.pt.ifBlank { card.example.orEmpty() }
    } else {
        card.ru.ifBlank { card.exampleTranslation.orEmpty() }
    }
    return main.trim().ifBlank { "—" }
}

@Composable
private fun StudioPlayFace(
    card: WordCard?,
    reveal: Int,
    dirPtToRu: Boolean,
    sourceLang: Int,
    targetLang: Int,
    strings: UiStrings,
    onToggleDir: () -> Unit,
    onTag: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    var dragX by remember(card?.id) { mutableFloatStateOf(0f) }
    val localDensity = LocalDensity.current
    val thresholdPx = with(localDensity) { 64.dp.toPx() }
    val dragState = rememberDraggableState { delta -> dragX += delta }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(300.dp)
            .graphicsLayer {
                translationX = dragX
                rotationY = (dragX / 48f).coerceIn(-12f, 12f)
                cameraDistance = 16f * density
            }
            .shadow(10.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x66000000), spotColor = Color(0x66000000))
            .clip(RoundedCornerShape(22.dp))
            .drawBehind { drawStudioNotebook() }
            .border(1.dp, StudioCardColors.Border, RoundedCornerShape(22.dp))
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    when {
                        dragX < -thresholdPx || velocity < -900f -> onNext()
                        dragX > thresholdPx || velocity > 900f -> onPrev()
                    }
                    dragX = 0f
                },
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StudioTagBadge(
                    tag = card?.partOfSpeech.orEmpty().ifBlank { "geral" },
                    strings = strings,
                    onTag = onTag,
                )
                StudioDirChip(
                    dirPtToRu = dirPtToRu,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    onClick = onToggleDir,
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (card != null) {
                    StudioCardContent(card = card, reveal = reveal, dirPtToRu = dirPtToRu)
                }
            }
        }
    }
}

@Composable
private fun StudioTagBadge(tag: String, strings: UiStrings, onTag: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Text(
            text = strings.studioTagLabel(tag),
            color = StudioCardColors.TagInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(StudioCardColors.TagBg, RoundedCornerShape(999.dp))
                .border(1.dp, StudioCardColors.TagBorder, RoundedCornerShape(999.dp))
                .clickable { open = true }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            StudioWordTags.forEach { key ->
                DropdownMenuItem(
                    text = { Text(strings.studioTagLabel(key)) },
                    onClick = {
                        onTag(key)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun StudioCardEditorDialog(
    card: WordCard,
    strings: UiStrings,
    onDismiss: () -> Unit,
    onSave: (WordCard) -> Unit,
) {
    var pt by remember(card.id) { mutableStateOf(card.pt) }
    var ru by remember(card.id) { mutableStateOf(card.ru) }
    var example by remember(card.id) { mutableStateOf(card.example.orEmpty()) }
    var exampleRu by remember(card.id) { mutableStateOf(card.exampleTranslation.orEmpty()) }
    var tag by remember(card.id) { mutableStateOf(card.partOfSpeech.orEmpty().ifBlank { "geral" }) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 420.dp, max = 640.dp)
                .background(PracticeSessionColors.Card, RoundedCornerShape(20.dp))
                .border(1.dp, PracticeSessionColors.Border, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = strings.studioEditCard,
                color = PracticeSessionColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StudioEditorField(strings.dictionaryWordPtLabel, pt, minLines = 2, maxLines = 6) { pt = it }
                StudioEditorField(strings.dictionaryWordRuLabel, ru, minLines = 2, maxLines = 6) { ru = it }
                StudioEditorField(strings.dictionaryWordExampleLabel, example, minLines = 4, maxLines = 10) { example = it }
                StudioEditorField(strings.studioSpeakExRu, exampleRu, minLines = 4, maxLines = 10) { exampleRu = it }
                StudioFieldLabel(strings.studioWordType)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StudioWordTags.forEach { key ->
                        Seg(label = strings.studioTagLabel(key), selected = tag == key) { tag = key }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PortTextButton(text = strings.studioCancel, onClick = onDismiss, emphasis = PortTextEmphasis.Muted)
                PortTextButton(
                    text = strings.studioSave,
                    onClick = {
                        onSave(
                            card.copy(
                                pt = pt.trim(),
                                ru = ru.trim(),
                                example = example.trim().ifEmpty { null },
                                exampleTranslation = exampleRu.trim().ifEmpty { null },
                                partOfSpeech = tag,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun StudioEditorField(
    label: String,
    value: String,
    minLines: Int,
    maxLines: Int,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StudioFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (24 * minLines + 32).dp, max = (24 * maxLines + 40).dp),
            minLines = minLines,
            maxLines = maxLines,
            singleLine = false,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
            ),
        )
    }
}

@Composable
private fun StudioCardBox(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PracticeSessionColors.Card, RoundedCornerShape(16.dp))
            .border(1.dp, PracticeSessionColors.Border, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) { content() }
}

@Composable
private fun StudioFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = PracticeSessionColors.TextFaint,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun StudioMixerLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = StudioCardColors.Muted,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun StudioSegBar(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    PortSegmentedControl(
        items = items,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
private fun StudioToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    PortChip(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun Seg(label: String, selected: Boolean, onClick: () -> Unit) {
    PortChip(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun StudioFader(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    val glow by animateFloatAsState(
        targetValue = if (dragging) 1f else 0.55f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.7f),
        label = "studioFaderGlow",
    )
    val fraction = (value / 5f).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = PracticeSessionColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = (8f * glow).dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = StudioCardColors.Cyan.copy(alpha = 0.45f * glow),
                        spotColor = StudioCardColors.Cyan.copy(alpha = 0.55f * glow),
                    )
                    .background(
                        StudioCardColors.Cyan.copy(alpha = 0.14f + 0.16f * glow),
                        RoundedCornerShape(8.dp),
                    )
                    .border(1.dp, StudioCardColors.Cyan.copy(alpha = 0.28f + 0.4f * glow), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = LocalUiStrings.current.studioPauseValue(value),
                    color = if (dragging) Color.White else StudioCardColors.Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(onChange) {
                    val padPx = 10.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        dragging = true
                        onChange(studioPauseFromX(down.position.x, size.width.toFloat(), padPx))
                        drag(down.id) { change ->
                            change.consume()
                            onChange(studioPauseFromX(change.position.x, size.width.toFloat(), padPx))
                        }
                        dragging = false
                    }
                }
                .drawBehind {
                    val padPx = 10.dp.toPx()
                    val usable = size.width - padPx * 2f
                    val ticks = 10
                    val y = size.height / 2f + 8.dp.toPx()
                    for (i in 0..ticks) {
                        val x = padPx + usable * (i / ticks.toFloat())
                        val major = i % 2 == 0
                        drawLine(
                            color = Color.White.copy(alpha = if (major) 0.28f else 0.12f),
                            start = Offset(x, y),
                            end = Offset(x, y + if (major) 5.dp.toPx() else 3.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx(),
                        )
                    }
                },
        ) {
            val pad = 10.dp
            val thumb = 18.dp
            val usable = (maxWidth - pad * 2).coerceAtLeast(1.dp)
            val thumbX = (pad + usable * fraction - thumb / 2).coerceIn(0.dp, maxWidth - thumb)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = pad)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(StudioCardColors.Border),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = pad)
                    .width((usable * fraction).coerceAtLeast(6.dp))
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF7EE887), StudioCardColors.Cyan, Color(0xFF9AF3FF)),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = thumbX)
                    .size(thumb)
                    .graphicsLayer {
                        scaleX = 0.92f + 0.12f * glow
                        scaleY = 0.92f + 0.12f * glow
                    }
                    .shadow(
                        elevation = (12f * glow).dp,
                        shape = CircleShape,
                        ambientColor = StudioCardColors.Cyan.copy(alpha = 0.65f * glow),
                        spotColor = StudioCardColors.Cyan.copy(alpha = 0.85f * glow),
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFB8FFC4), StudioCardColors.Cyan),
                        ),
                        CircleShape,
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            )
        }
    }
}

private fun studioPauseFromX(x: Float, width: Float, pad: Float): Float {
    val usable = (width - pad * 2f).coerceAtLeast(1f)
    val t = ((x - pad) / usable).coerceIn(0f, 1f)
    return ((t * 10f).roundToInt() / 2f).coerceIn(0f, 5f)
}
