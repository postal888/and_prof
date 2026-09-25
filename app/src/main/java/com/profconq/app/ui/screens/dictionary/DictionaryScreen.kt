package com.profconq.app.ui.screens.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import android.content.Context
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.content.Intent
import com.profconq.app.data.model.Collection
import com.profconq.app.data.model.VocabLearnMark
import com.profconq.app.data.model.WordCard
import com.profconq.app.media.AudioRecorder
import com.profconq.app.media.deleteAudioFile
import com.profconq.app.media.formatAudioTime
import com.profconq.app.media.playAudioFile
import kotlinx.coroutines.delay
import com.profconq.app.media.rememberRecordAudioPermission
import com.profconq.app.media.showAudioExportChooser
import com.profconq.app.ui.components.AudioTrimBottomSheet
import com.profconq.app.ui.components.AudioTrimResult
import com.profconq.app.ui.components.AudioRecordingsList
import com.profconq.app.ui.components.GradientPrimaryButton
import com.profconq.app.ui.components.GlassOutlineButton
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.PortLayout
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.components.VocabLampsRow
import com.profconq.app.ui.components.glassCard
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalStudyLanguagePrefs
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpNeonGreen
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.study.StudyWordSelectionStore

private sealed interface DictionaryTableRow {
    data class Group(val collection: Collection) : DictionaryTableRow
    data class WordRow(
        val index: Int,
        val collection: Collection,
        val card: WordCard,
    ) : DictionaryTableRow
    data class NewWord(val collection: Collection) : DictionaryTableRow
}

private sealed interface FolderDialogState {
    data object Create : FolderDialogState
    data class Edit(val collection: Collection) : FolderDialogState
}

private sealed interface DictionaryRecordingTarget {
    data class Folder(val collectionId: String) : DictionaryRecordingTarget
    data class Word(val card: WordCard) : DictionaryRecordingTarget
}

private sealed interface DictionaryAudioTrimTarget {
    val path: String
    val fresh: Boolean

    data class Word(
        val card: WordCard,
        override val path: String,
        override val fresh: Boolean,
    ) : DictionaryAudioTrimTarget

    data class Folder(
        val collectionId: String,
        override val path: String,
        override val fresh: Boolean,
    ) : DictionaryAudioTrimTarget
}

@Composable
fun DictionaryScreen(
    collections: List<Collection>,
    onCreateCollection: (title: String, description: String?, onCreated: (String) -> Unit) -> Unit,
    onUpdateCollectionTitle: (String, String) -> Unit,
    onUpdateCollectionDescription: (String, String?) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onAppendFolderAudio: (String, String) -> Unit,
    onSetFolderAudios: (String, List<String>, Map<String, String>) -> Unit,
    onSetFolderAudioLabel: (String, String, String) -> Unit,
    onAddCard: (collectionId: String, pt: String, ru: String, example: String?) -> Unit,
    onUpdateCard: (WordCard) -> Unit,
    onDeleteCard: (String) -> Unit,
    onSetLearnMark: (String, String) -> Unit,
    onOpenCardEditor: (collectionId: String, cardId: String) -> Unit,
    onBack: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selectionStore: StudyWordSelectionStore? = null,
    onSelectionDone: (() -> Unit)? = null,
    onSelectionCancel: (() -> Unit)? = null,
    onSendToStudio: (List<String>) -> Unit = {},
    wordLimitMessage: String? = null,
    onDismissWordLimitMessage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<WordCard?>(null) }
    var folderDialog by remember { mutableStateOf<FolderDialogState?>(null) }
    var pendingDeleteFolder by remember { mutableStateOf<Collection?>(null) }
    var addWordCollectionId by remember { mutableStateOf<String?>(null) }
    var collapsedFolderIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var compactWordRows by rememberSaveable { mutableStateOf(false) }
    val bulkSelection = remember { StudyWordSelectionStore() }

    val filteredCollections = remember(collections, filterCollectionId) {
        if (filterCollectionId == null) collections
        else collections.filter { it.id == filterCollectionId }
    }

    val searching = searchQuery.trim().isNotEmpty()
    val tableRows = remember(filteredCollections, searchQuery, selectionMode, collapsedFolderIds, compactWordRows) {
        buildDictionaryTableRows(
            collections = filteredCollections,
            searchQuery = searchQuery,
            includeNewWords = !selectionMode && !compactWordRows,
            collapsedIds = if (searching) emptySet() else collapsedFolderIds.toSet(),
        )
    }

    val activeSelectionStore = if (selectionMode) selectionStore else bulkSelection
    val selectedIdsFlow = activeSelectionStore?.selectedIds
    val selectedIds by if (selectedIdsFlow != null) {
        selectedIdsFlow.collectAsState()
    } else {
        remember { mutableStateOf(emptySet<String>()) }
    }

    val wordCount = remember(filteredCollections, searchQuery) {
        val q = searchQuery.trim().lowercase()
        filteredCollections.sumOf { folder ->
            folder.cards.count { card -> q.isEmpty() || matchesSearch(card, q) }
        }
    }

    val context = LocalContext.current
    val audioPermission = rememberRecordAudioPermission()
    val recorder = remember { AudioRecorder(context) }
    var recordingTarget by remember { mutableStateOf<DictionaryRecordingTarget?>(null) }
    var recordingStartedAt by remember { mutableStateOf<Long?>(null) }
    var audioTrimTarget by remember { mutableStateOf<DictionaryAudioTrimTarget?>(null) }

    fun finishRecording() {
        recordingStartedAt = null
        if (!recorder.isRecording) {
            recordingTarget = null
            return
        }
        val path = recorder.stop() ?: run {
            recordingTarget = null
            return
        }
        when (val target = recordingTarget) {
            is DictionaryRecordingTarget.Folder ->
                audioTrimTarget = DictionaryAudioTrimTarget.Folder(target.collectionId, path, fresh = true)
            is DictionaryRecordingTarget.Word ->
                audioTrimTarget = DictionaryAudioTrimTarget.Word(target.card, path, fresh = true)
            null -> Unit
        }
        recordingTarget = null
    }

    fun dismissAudioTrim(target: DictionaryAudioTrimTarget) {
        if (target.fresh) deleteAudioFile(target.path)
        audioTrimTarget = null
    }

    fun applyAudioTrimResult(target: DictionaryAudioTrimTarget, result: AudioTrimResult) {
        when (result) {
            is AudioTrimResult.Saved -> {
                val newPath = result.path
                when (target) {
                    is DictionaryAudioTrimTarget.Word -> {
                        val paths = target.card.resolvedAudioPaths()
                        if (target.fresh) {
                            if (newPath != target.path) deleteAudioFile(target.path)
                            onUpdateCard(
                                target.card.copy(
                                    audioPaths = paths + newPath,
                                    audioPath = (paths + newPath).firstOrNull(),
                                ),
                            )
                        } else {
                            target.path.let { old ->
                                if (old != newPath) deleteAudioFile(old)
                            }
                            val updatedPaths = paths.map { if (it == target.path) newPath else it }
                            val updatedLabels = target.card.audioLabels.toMutableMap().apply {
                                remove(target.path)?.let { put(newPath, it) }
                            }
                            onUpdateCard(
                                target.card.copy(
                                    audioPaths = updatedPaths,
                                    audioPath = updatedPaths.firstOrNull(),
                                    audioLabels = updatedLabels,
                                ),
                            )
                        }
                    }
                    is DictionaryAudioTrimTarget.Folder -> {
                        val collection = collections.find { it.id == target.collectionId }
                        val paths = collection?.resolvedFolderAudioPaths() ?: emptyList()
                        if (target.fresh) {
                            if (newPath != target.path) deleteAudioFile(target.path)
                            onAppendFolderAudio(target.collectionId, newPath)
                        } else {
                            target.path.let { old ->
                                if (old != newPath) deleteAudioFile(old)
                            }
                            val updated = paths.map { if (it == target.path) newPath else it }
                            onSetFolderAudios(
                                target.collectionId,
                                updated,
                                mapOf(target.path to newPath),
                            )
                        }
                    }
                }
            }
            AudioTrimResult.Deleted -> {
                deleteAudioFile(target.path)
                when (target) {
                    is DictionaryAudioTrimTarget.Word -> {
                        val updated = target.card.resolvedAudioPaths().filter { it != target.path }
                        val updatedLabels = target.card.audioLabels.filterKeys { it != target.path }
                        onUpdateCard(
                            target.card.copy(
                                audioPaths = updated,
                                audioPath = updated.firstOrNull(),
                                audioLabels = updatedLabels,
                            ),
                        )
                    }
                    is DictionaryAudioTrimTarget.Folder -> {
                        val collection = collections.find { it.id == target.collectionId }
                        val updated = collection?.resolvedFolderAudioPaths()?.filter { it != target.path }
                            ?: emptyList()
                        onSetFolderAudios(target.collectionId, updated, emptyMap())
                    }
                }
            }
        }
        audioTrimTarget = null
    }

    fun toggleFolderRecording(collectionId: String) {
        audioPermission.runWithPermission {
            val current = recordingTarget
            if (current is DictionaryRecordingTarget.Folder && current.collectionId == collectionId) {
                finishRecording()
                return@runWithPermission
            }
            if (recorder.isRecording) finishRecording()
            try {
                recorder.start("folder-$collectionId")
                recordingTarget = DictionaryRecordingTarget.Folder(collectionId)
                recordingStartedAt = System.currentTimeMillis()
            } catch (_: Exception) {
                recordingTarget = null
                recordingStartedAt = null
            }
        }
    }

    fun toggleWordRecording(card: WordCard) {
        audioPermission.runWithPermission {
            val current = recordingTarget
            if (current is DictionaryRecordingTarget.Word && current.card.id == card.id) {
                finishRecording()
                return@runWithPermission
            }
            if (recorder.isRecording) finishRecording()
            try {
                recorder.start("card-${card.id}")
                recordingTarget = DictionaryRecordingTarget.Word(card)
            } catch (_: Exception) {
                recordingTarget = null
            }
        }
    }

    fun isFolderRecording(collectionId: String): Boolean =
        recordingTarget is DictionaryRecordingTarget.Folder &&
            (recordingTarget as DictionaryRecordingTarget.Folder).collectionId == collectionId

    fun folderRecordingStartedAt(collectionId: String): Long? =
        recordingStartedAt?.takeIf { isFolderRecording(collectionId) }

    fun isWordRecording(cardId: String): Boolean =
        recordingTarget is DictionaryRecordingTarget.Word &&
            (recordingTarget as DictionaryRecordingTarget.Word).card.id == cardId

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) recorder.stop()
        }
    }

    val strings = LocalUiStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = PortLayout.Gutter),
    ) {
        TabScreenHeader(
            tab = MainTab.Dictionary,
            subtitle = strings.dictionarySubtitle(wordCount),
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(PortLayout.HeaderToContent))

        wordLimitMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = PpDanger,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissWordLimitMessage() },
            )
        }

        if (selectionMode) {
            SelectionModeBanner(selectedCount = selectedIds.size)
        }

        if (!selectionMode) {
            DictionaryActionBar(
                filterCollectionId = filterCollectionId,
                onCreateFolder = { folderDialog = FolderDialogState.Create },
                onAddWord = {
                    val id = filterCollectionId ?: collections.firstOrNull()?.id
                    if (id != null) addWordCollectionId = id
                },
                addWordEnabled = filterCollectionId != null || collections.size == 1,
            )
        }

        if (collections.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            MutedText(strings.dictionaryEmptyHint)
            Spacer(modifier = Modifier.height(12.dp))
            GradientPrimaryButton(
                text = strings.dictionaryNewFolder,
                onClick = { folderDialog = FolderDialogState.Create },
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = PpBrandNavy)
                },
            )
        } else {
            DictionaryToolbar(
                collections = collections,
                filterCollectionId = filterCollectionId,
                onFilterCollectionId = { filterCollectionId = it },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                filteredCollections = filteredCollections,
                onUpdateCollectionTitle = onUpdateCollectionTitle,
                onEditFolder = { folderDialog = FolderDialogState.Edit(it) },
                onDeleteFolder = { pendingDeleteFolder = it },
                isFolderRecording = ::isFolderRecording,
                folderRecordingStartedAt = ::folderRecordingStartedAt,
                onToggleFolderRecording = ::toggleFolderRecording,
                onTrimFolderAudio = { collectionId, path ->
                    audioTrimTarget = DictionaryAudioTrimTarget.Folder(collectionId, path, fresh = false)
                },
                onSetFolderAudioLabel = onSetFolderAudioLabel,
                selectionMode = selectionMode,
            )

            Spacer(modifier = Modifier.height(8.dp))
            DictionaryCardsCollapseBar(
                compact = compactWordRows,
                onToggle = { compactWordRows = !compactWordRows },
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (tableRows.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                MutedText(strings.dictionaryNoSearchResults)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (compactWordRows) 3.dp else 8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        items = tableRows,
                        key = { row ->
                            when (row) {
                                is DictionaryTableRow.Group -> "g-${row.collection.id}"
                                is DictionaryTableRow.WordRow -> "w-${row.card.id}"
                                is DictionaryTableRow.NewWord -> "n-${row.collection.id}"
                            }
                        },
                    ) { row ->
                        when (row) {
                            is DictionaryTableRow.Group -> {
                                val folderCardIds = visibleCardIdsInFolder(
                                    collection = row.collection,
                                    searchQuery = searchQuery,
                                )
                                val expanded = searching || row.collection.id !in collapsedFolderIds
                                DictionaryGroupCard(
                                    collection = row.collection,
                                    expanded = expanded,
                                    cardIds = folderCardIds,
                                    selectedIds = selectedIds,
                                    showManageActions = !selectionMode,
                                    onToggleExpand = {
                                        collapsedFolderIds = if (row.collection.id in collapsedFolderIds) {
                                            collapsedFolderIds - row.collection.id
                                        } else {
                                            collapsedFolderIds + row.collection.id
                                        }
                                    },
                                    onToggleFolder = { selectAll ->
                                        activeSelectionStore?.setFolder(folderCardIds, selectAll)
                                    },
                                    onEdit = { folderDialog = FolderDialogState.Edit(row.collection) },
                                    onDelete = { pendingDeleteFolder = row.collection },
                                    onAddWord = { addWordCollectionId = row.collection.id },
                                )
                            }
                            is DictionaryTableRow.WordRow -> {
                                DictionaryWordCard(
                                    index = row.index,
                                    collection = row.collection,
                                    card = row.card,
                                    compact = compactWordRows,
                                    checked = row.card.id in selectedIds,
                                    showActions = !selectionMode,
                                    fieldsEditable = !selectionMode,
                                    isRecording = isWordRecording(row.card.id),
                                    onToggleSelected = { activeSelectionStore?.toggle(row.card.id) },
                                    onToggleRecording = { toggleWordRecording(row.card) },
                                    onDelete = { pendingDelete = row.card },
                                    onSetLearnMark = onSetLearnMark,
                                    onUpdateCard = onUpdateCard,
                                    onOpenCardEditor = {
                                        if (selectionMode) activeSelectionStore?.toggle(row.card.id)
                                        else onOpenCardEditor(row.collection.id, row.card.id)
                                    },
                                    onTrimAudio = { path ->
                                        audioTrimTarget = DictionaryAudioTrimTarget.Word(
                                            row.card,
                                            path,
                                            fresh = false,
                                        )
                                    },
                                )
                            }
                            is DictionaryTableRow.NewWord -> DictionaryNewWordCard(
                                collection = row.collection,
                                onAdd = { pt, ru, ex ->
                                    onAddCard(row.collection.id, pt, ru, ex)
                                },
                            )
                        }
                    }
                }
            }
        }

        if (selectionMode) {
            SelectionModeBottomBar(
                selectedCount = selectedIds.size,
                onCancel = { onSelectionCancel?.invoke() },
                onDone = { onSelectionDone?.invoke() },
            )
        } else if (selectedIds.isNotEmpty()) {
            DictionaryBulkBar(
                selectedCount = selectedIds.size,
                onSendToStudio = {
                    onSendToStudio(selectedIds.toList())
                    bulkSelection.clear()
                },
                onExport = {
                    val cards = collections
                        .flatMap { folder -> folder.cards }
                        .filter { it.id in selectedIds }
                    shareDictionaryWords(context, cards, strings.dictionaryExportWords)
                },
                onClear = { bulkSelection.clear() },
            )
        }
    }

    if (!selectionMode) {
        when (val dialog = folderDialog) {
        FolderDialogState.Create -> FolderEditDialog(
            collection = null,
            onDismiss = { folderDialog = null },
            onConfirm = { title, description ->
                onCreateCollection(title, description) { newId ->
                    filterCollectionId = newId
                }
            },
        )
        is FolderDialogState.Edit -> FolderEditDialog(
            collection = dialog.collection,
            onDismiss = { folderDialog = null },
            onConfirm = { title, description ->
                onUpdateCollectionTitle(dialog.collection.id, title)
                onUpdateCollectionDescription(dialog.collection.id, description)
            },
        )
        null -> Unit
        }
    }

    if (!selectionMode) addWordCollectionId?.let { collectionId ->
        val collection = collections.find { it.id == collectionId }
        if (collection != null) {
            AddWordDialog(
                collectionTitle = collection.title,
                onDismiss = { addWordCollectionId = null },
                onConfirm = { pt, ru, example ->
                    onAddCard(collectionId, pt, ru, example)
                },
            )
        }
    }

    if (!selectionMode) pendingDeleteFolder?.let { collection ->
        DeleteFolderDialog(
            collection = collection,
            onDismiss = { pendingDeleteFolder = null },
            onConfirm = {
                onDeleteCollection(collection.id)
                if (filterCollectionId == collection.id) {
                    filterCollectionId = null
                }
            },
        )
    }

    if (!selectionMode) pendingDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings.dictionaryDeleteWordTitle, color = PpHeading) },
            text = { Text("${card.pt} — ${card.ru}", color = PpText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCard(card.id)
                        pendingDelete = null
                    },
                ) {
                    Text(strings.delete, color = PpAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.cancel, color = PpTextMuted)
                }
            },
            containerColor = PpSurface,
        )
    }

    if (!selectionMode) {
        audioTrimTarget?.let { target ->
            val prefix = when (target) {
                is DictionaryAudioTrimTarget.Word -> "card-${target.card.id}"
                is DictionaryAudioTrimTarget.Folder -> "folder-${target.collectionId}"
            }
            AudioTrimBottomSheet(
                audioPath = target.path,
                filePrefix = prefix,
                onDismiss = { dismissAudioTrim(target) },
                onResult = { applyAudioTrimResult(target, it) },
            )
        }
    }
}

@Composable
private fun DictionaryCardsCollapseBar(
    compact: Boolean,
    onToggle: () -> Unit,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (compact) strings.dictionaryExpandCards else strings.dictionaryCompactCards,
            style = MaterialTheme.typography.labelLarge,
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = if (compact) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = if (compact) strings.dictionaryExpandCards else strings.dictionaryCompactCards,
            tint = PpHeading,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun DictionaryActionBar(
    filterCollectionId: String?,
    onCreateFolder: () -> Unit,
    onAddWord: () -> Unit,
    addWordEnabled: Boolean,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GradientPrimaryButton(
            text = strings.dictionaryNewFolder,
            onClick = onCreateFolder,
            compact = true,
            modifier = Modifier.weight(1f),
            leading = {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = PpBrandNavy, modifier = Modifier.size(17.dp))
            },
        )
        GlassOutlineButton(
            text = strings.dictionaryAddWord,
            onClick = onAddWord,
            enabled = addWordEnabled,
            compact = true,
            modifier = Modifier.weight(1f),
            leading = {
                Icon(Icons.Default.Add, contentDescription = null, tint = PpNeonGreen, modifier = Modifier.size(17.dp))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DictionaryToolbar(
    collections: List<Collection>,
    filterCollectionId: String?,
    onFilterCollectionId: (String?) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredCollections: List<Collection>,
    onUpdateCollectionTitle: (String, String) -> Unit,
    onEditFolder: (Collection) -> Unit,
    onDeleteFolder: (Collection) -> Unit,
    isFolderRecording: (String) -> Boolean,
    folderRecordingStartedAt: (String) -> Long?,
    onToggleFolderRecording: (String) -> Unit,
    onTrimFolderAudio: (collectionId: String, path: String) -> Unit,
    onSetFolderAudioLabel: (collectionId: String, path: String, label: String) -> Unit,
    selectionMode: Boolean = false,
) {
    val strings = LocalUiStrings.current
    val context = LocalContext.current
    var collectionMenuExpanded by remember { mutableStateOf(false) }
    var editingFolderTitle by rememberSaveable { mutableStateOf(false) }
    var titleDraft by rememberSaveable { mutableStateOf("") }
    var filtersExpanded by rememberSaveable { mutableStateOf(true) }

    val activeCollection = filterCollectionId?.let { id -> collections.find { it.id == id } }

    LaunchedEffect(activeCollection?.id, activeCollection?.title) {
        titleDraft = activeCollection?.title.orEmpty()
    }

    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { filtersExpanded = !filtersExpanded }
                .background(PpSurfaceInput)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.dictionarySearchAndFolders,
                    style = MaterialTheme.typography.labelLarge,
                    color = PpHeading,
                    fontWeight = FontWeight.Medium,
                )
                if (!filtersExpanded) {
                    Text(
                        text = buildString {
                            if (searchQuery.isNotBlank()) {
                                append("«")
                                append(searchQuery.take(24))
                                if (searchQuery.length > 24) append("…")
                                append("» · ")
                            }
                            append(activeCollection?.title ?: strings.dictionaryAllFolders)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = PpTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                imageVector = if (filtersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PpAccent,
                modifier = Modifier.size(22.dp),
            )
        }

        AnimatedVisibility(
            visible = filtersExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 46.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = {
                        Text(
                            strings.dictionarySearchTablePlaceholder,
                            color = PpTextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = searchFieldColors(),
                )

                ExposedDropdownMenuBox(
                    expanded = collectionMenuExpanded,
                    onExpandedChange = { collectionMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = activeCollection?.title ?: strings.dictionaryAllFolders,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .heightIn(max = 46.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collectionMenuExpanded) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(10.dp),
                        colors = searchFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = collectionMenuExpanded,
                        onDismissRequest = { collectionMenuExpanded = false },
                        containerColor = PpSurface,
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.dictionaryAllFolders, color = PpText) },
                            onClick = {
                                onFilterCollectionId(null)
                                collectionMenuExpanded = false
                            },
                        )
                        collections.forEach { col ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${col.title} (${col.cards.size})",
                                        color = PpText,
                                        maxLines = 1,
                                    )
                                },
                                onClick = {
                                    onFilterCollectionId(col.id)
                                    collectionMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                if (activeCollection != null && !selectionMode) {
                    if (editingFolderTitle) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = titleDraft,
                                onValueChange = { titleDraft = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(max = 46.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = searchFieldColors(),
                                shape = RoundedCornerShape(8.dp),
                            )
                            Button(
                                onClick = {
                                    onUpdateCollectionTitle(activeCollection.id, titleDraft)
                                    editingFolderTitle = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PpAccent,
                                    contentColor = PpBrandNavy,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(strings.commonOk, color = PpBrandNavy, style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = { editingFolderTitle = false },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text(strings.cancel, color = PpText, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { onEditFolder(activeCollection) },
                                colors = ButtonDefaults.buttonColors(containerColor = PpSurfaceInput),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    strings.dictionaryEditFolderTitle,
                                    color = PpText,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            Button(
                                onClick = { onDeleteFolder(activeCollection) },
                                colors = ButtonDefaults.buttonColors(containerColor = PpSurfaceInput),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    strings.delete,
                                    color = PpText,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!selectionMode && collections.isNotEmpty()) {
            val recordingFolderId = collections.firstOrNull { isFolderRecording(it.id) }?.id
            val recordFolder = activeCollection
                ?: collections.singleOrNull()
                ?: filteredCollections.singleOrNull()
                ?: recordingFolderId?.let { id -> collections.find { it.id == id } }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FolderRecordButton(
                        isRecording = recordFolder?.let { isFolderRecording(it.id) } == true,
                        onClick = {
                            if (recordFolder != null) {
                                onToggleFolderRecording(recordFolder.id)
                            } else {
                                filtersExpanded = true
                                collectionMenuExpanded = true
                            }
                        },
                    )
                    if (recordFolder == null) {
                        MutedText(
                            text = strings.dictionarySelectFolderToRecord,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                recordFolder?.let { folder ->
                    FolderRecordingPlayerSection(
                        isRecording = isFolderRecording(folder.id),
                        recordingStartedAtMs = folderRecordingStartedAt(folder.id),
                        folder = folder,
                        onShare = { path -> showAudioExportChooser(context, path, strings.audioExportLabels()) },
                        onTrim = { path -> onTrimFolderAudio(folder.id, path) },
                        onTitleChange = { path, title ->
                            onSetFolderAudioLabel(folder.id, path, title)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalUiStrings.current
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) PpDanger else PpSurfaceInput,
            contentColor = if (isRecording) PpHeading else PpText,
        ),
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isRecording) PpHeading else PpAccent,
        )
        Text(
            text = if (isRecording) strings.dictionaryStop else strings.dictionaryRecordFolder,
            color = if (isRecording) PpHeading else PpText,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun RecordingElapsedTimer(
    startedAtMs: Long,
    modifier: Modifier = Modifier,
) {
    var tick by remember(startedAtMs) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMs) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(250)
        }
    }
    val elapsedMs = (tick - startedAtMs).coerceAtLeast(0L).toInt()
    Text(
        text = formatAudioTime(elapsedMs),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = PpDanger,
        modifier = modifier,
    )
}

@Composable
private fun FolderRecordingPlayerSection(
    isRecording: Boolean,
    recordingStartedAtMs: Long?,
    folder: Collection,
    onShare: (String) -> Unit,
    onTrim: (String) -> Unit,
    onTitleChange: (path: String, title: String) -> Unit,
) {
    val strings = LocalUiStrings.current
    val folderAudioPaths = folder.resolvedFolderAudioPaths()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PpDanger.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = PpDanger,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = strings.dictionaryRecording,
                    style = MaterialTheme.typography.labelMedium,
                    color = PpHeading,
                    fontWeight = FontWeight.SemiBold,
                )
                if (recordingStartedAtMs != null) {
                    RecordingElapsedTimer(startedAtMs = recordingStartedAtMs)
                }
            }
        }
        if (folderAudioPaths.isNotEmpty()) {
            AudioRecordingsList(
                audioPaths = folderAudioPaths,
                titleForPath = { path, index ->
                    folder.folderAudioTitle(path, index, strings::dictionaryFolderRecordingDefault)
                },
                onShare = onShare,
                onTrim = onTrim,
                onTitleChange = onTitleChange,
            )
        }
    }
}

@Composable
private fun DictionaryGroupCard(
    collection: Collection,
    expanded: Boolean,
    cardIds: List<String>,
    selectedIds: Set<String>,
    showManageActions: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFolder: (selectAll: Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddWord: () -> Unit,
) {
    val strings = LocalUiStrings.current
    val source = sourceTypeLabel(collection.sourceType, strings)
    val selectedInFolder = cardIds.count { it in selectedIds }
    val toggleState = when {
        cardIds.isEmpty() -> ToggleableState.Off
        selectedInFolder == 0 -> ToggleableState.Off
        selectedInFolder == cardIds.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 12.dp)
            .background(PpAccent.copy(alpha = 0.10f))
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = toggleState,
            onClick = { onToggleFolder(toggleState != ToggleableState.On) },
            enabled = cardIds.isNotEmpty(),
            colors = CheckboxDefaults.colors(checkedColor = PpAccent),
        )
        IconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = if (expanded) strings.dictionaryCollapseFolder else strings.dictionaryExpandFolder,
                tint = PpHeading,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToggleExpand)
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = "${collection.title} · ${collection.cards.size}",
                style = MaterialTheme.typography.labelLarge,
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (source.isNotEmpty()) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelSmall,
                    color = PpTextMuted,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (showManageActions) {
            IconButton(onClick = onAddWord, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, contentDescription = strings.dictionaryAddWord, tint = PpAccent)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = strings.dictionaryEditFolderTitle, tint = PpTextMuted)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = PpTextMuted)
            }
        }
    }
}

@Composable
private fun DictionaryNewWordCard(
    collection: Collection,
    onAdd: (pt: String, ru: String, example: String?) -> Unit,
) {
    val strings = LocalUiStrings.current
    val studyLangs = LocalStudyLanguagePrefs.current
    var pt by rememberSaveable(collection.id) { mutableStateOf("") }
    var ru by rememberSaveable(collection.id) { mutableStateOf("") }
    var example by rememberSaveable(collection.id) { mutableStateOf("") }
    val canAdd = pt.trim().isNotEmpty() && ru.trim().isNotEmpty()

    PortCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.dictionaryAddWord,
                style = MaterialTheme.typography.labelLarge,
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = pt,
                onValueChange = { pt = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        strings.studyLanguageName(studyLangs.source),
                        color = PpTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = searchFieldColors(),
            )
            OutlinedTextField(
                value = ru,
                onValueChange = { ru = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        strings.studyLanguageName(studyLangs.target),
                        color = PpTextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = searchFieldColors(),
            )
            OutlinedTextField(
                value = example,
                onValueChange = { example = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 8,
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = {
                    Text(
                        strings.dictionaryWordExampleLabel,
                        color = PpTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = searchFieldColors(),
            )
            GradientPrimaryButton(
                text = strings.dictionaryAddWord,
                onClick = {
                    val trimmedPt = pt.trim()
                    val trimmedRu = ru.trim()
                    if (trimmedPt.isNotEmpty() && trimmedRu.isNotEmpty()) {
                        onAdd(trimmedPt, trimmedRu, example.trim().ifEmpty { null })
                        pt = ""
                        ru = ""
                        example = ""
                    }
                },
                enabled = canAdd,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Reserved slot for a 3-digit index so it never collides with the word. */
private val DictionaryCardIndexWidth = 36.dp

@Composable
private fun DictionaryWordCard(
    index: Int,
    collection: Collection,
    card: WordCard,
    compact: Boolean,
    checked: Boolean,
    showActions: Boolean,
    fieldsEditable: Boolean,
    isRecording: Boolean,
    onToggleSelected: () -> Unit,
    onToggleRecording: () -> Unit,
    onTrimAudio: (String) -> Unit,
    onDelete: () -> Unit,
    onSetLearnMark: (String, String) -> Unit,
    onUpdateCard: (WordCard) -> Unit,
    onOpenCardEditor: () -> Unit,
) {
    val context = LocalContext.current
    val strings = LocalUiStrings.current
    val firstAudio = card.resolvedAudioPaths().firstOrNull()
    val tag = card.partOfSpeech?.trim().orEmpty().let { raw ->
        if (raw.isEmpty() || raw.equals("geral", ignoreCase = true)) ""
        else strings.studioTagLabel(raw)
    }
    val example = card.example.orEmpty()
    val exampleRu = card.exampleTranslation.orEmpty()

    if (compact) {
        PortCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DictionaryCardIndex(
                    index = index,
                    checked = checked,
                    onToggle = onToggleSelected,
                )
                Text(
                    text = card.pt.ifBlank { strings.dictionaryWordPtLabel },
                    color = if (card.pt.isBlank()) PpTextMuted else PpHeading,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenCardEditor),
                )
            }
        }
        return
    }

    PortCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DictionaryCardIndex(
                    index = index,
                    checked = checked,
                    onToggle = onToggleSelected,
                )
                VocabCardField(
                    text = card.pt,
                    placeholder = strings.dictionaryWordPtLabel,
                    emphasized = true,
                    enabled = fieldsEditable,
                    minLines = 1,
                    maxLines = 8,
                    onSave = { next ->
                        if (next != card.pt) onUpdateCard(card.copy(pt = next))
                    },
                    modifier = Modifier.weight(1f),
                )
                if (tag.isNotEmpty()) {
                    Text(
                        text = tag,
                        color = PpHeading,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(PpAccent.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            VocabCardField(
                text = card.ru,
                placeholder = strings.dictionaryWordRuLabel,
                enabled = fieldsEditable,
                minLines = 1,
                maxLines = 8,
                onSave = { next ->
                    if (next != card.ru) onUpdateCard(card.copy(ru = next))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            VocabCardField(
                text = example,
                placeholder = strings.dictionaryWordExampleLabel,
                italic = true,
                tinted = example.isNotBlank(),
                enabled = fieldsEditable,
                minLines = 2,
                maxLines = 12,
                onSave = { next ->
                    val stored = next.ifEmpty { null }
                    if (stored != card.example) onUpdateCard(card.copy(example = stored))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            VocabCardField(
                text = exampleRu,
                placeholder = strings.dictionaryWordExampleRuLabel,
                italic = true,
                muted = true,
                enabled = fieldsEditable,
                minLines = 2,
                maxLines = 12,
                onSave = { next ->
                    val stored = next.ifEmpty { null }
                    if (stored != card.exampleTranslation) {
                        onUpdateCard(card.copy(exampleTranslation = stored))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VocabLampsRow(
                    level = VocabLearnMark.level(card.learnMark, card.known, card.due),
                    onSelect = { onSetLearnMark(card.id, it.toString()) },
                )
                Spacer(modifier = Modifier.weight(1f))
                if (showActions) {
                    if (firstAudio != null) {
                        IconButton(onClick = { playAudioFile(firstAudio) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.VolumeUp, contentDescription = strings.commonListen, tint = PpAccent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onTrimAudio(firstAudio) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCut, contentDescription = strings.audioTrimEdit, tint = PpTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onToggleRecording, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = if (isRecording) strings.dictionaryStop else strings.dictionaryRecord,
                            tint = if (isRecording) PpAccent else PpTextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    firstAudio?.let { path ->
                        IconButton(
                            onClick = { showAudioExportChooser(context, path, strings.audioExportLabels()) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = strings.commonExport, tint = PpTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onOpenCardEditor, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = strings.dictionaryCardEditor, tint = PpTextMuted, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = PpTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryCardIndex(
    index: Int,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = PpAccent,
                    uncheckedColor = PpTextMuted,
                    checkmarkColor = PpBrandNavy,
                ),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = index.toString(),
            color = PpTextMuted,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.width(DictionaryCardIndexWidth),
        )
    }
}

@Composable
private fun VocabCardField(
    text: String,
    placeholder: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    italic: Boolean = false,
    tinted: Boolean = false,
    muted: Boolean = false,
    enabled: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 8,
) {
    var value by remember { mutableStateOf(text) }
    var focused by remember { mutableStateOf(false) }
    val latest = remember { FieldDraft(text) }
    latest.value = value
    latest.saved = text
    LaunchedEffect(text) {
        if (!focused) value = text
    }
    DisposableEffect(Unit) {
        onDispose {
            val next = latest.value.trim()
            if (next != latest.saved) onSave(next)
        }
    }
    val shape = RoundedCornerShape(8.dp)
    val border = when {
        focused -> PpAccent.copy(alpha = 0.7f)
        tinted -> PpNeonGreen.copy(alpha = 0.28f)
        else -> PpBorder
    }
    val bg = when {
        tinted -> PpNeonGreen.copy(alpha = 0.08f)
        else -> PpSurfaceInput.copy(alpha = 0.55f)
    }
    val baseStyle = if (emphasized) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
    val textStyle = baseStyle.copy(
        color = when {
            value.isBlank() -> PpTextMuted.copy(alpha = 0.55f)
            muted -> PpTextMuted
            emphasized -> PpHeading
            else -> PpText
        },
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        enabled = enabled,
        textStyle = textStyle,
        minLines = minLines,
        maxLines = maxLines,
        cursorBrush = SolidColor(PpAccent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,
        ),
        modifier = modifier
            .onFocusChanged { state ->
                val now = state.isFocused
                if (focused && !now) {
                    val next = value.trim()
                    if (next != text) onSave(next)
                }
                focused = now
            }
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = PpTextMuted.copy(alpha = 0.55f),
                        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                        style = baseStyle,
                    )
                }
                inner()
            }
        },
    )
}

private class FieldDraft(var saved: String) {
    var value: String = saved
}

@Composable
private fun SelectionModeBanner(selectedCount: Int) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PpAccent.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = strings.dictionaryPickWordsForSet(selectedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SelectionModeBottomBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassOutlineButton(
            text = strings.cancel,
            onClick = onCancel,
            compact = true,
            modifier = Modifier.weight(1f),
        )
        GradientPrimaryButton(
            text = strings.dictionaryDoneCount(selectedCount),
            onClick = onDone,
            compact = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DictionaryBulkBar(
    selectedCount: Int,
    onSendToStudio: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
) {
    val strings = LocalUiStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = strings.dictionarySelectedCount(selectedCount),
                style = MaterialTheme.typography.bodyMedium,
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) {
                Text(strings.clear, color = PpTextMuted)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GradientPrimaryButton(
                text = strings.dictionarySendToStudio,
                onClick = onSendToStudio,
                compact = true,
                modifier = Modifier.weight(1f),
            )
            GlassOutlineButton(
                text = strings.dictionaryExportWords,
                onClick = onExport,
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun visibleCardIdsInFolder(collection: Collection, searchQuery: String): List<String> {
    val q = searchQuery.trim().lowercase()
    return collection.cards
        .filter { card -> q.isEmpty() || matchesSearch(card, q) }
        .map { it.id }
}

private fun buildDictionaryTableRows(
    collections: List<Collection>,
    searchQuery: String,
    includeNewWords: Boolean = true,
    collapsedIds: Set<String> = emptySet(),
): List<DictionaryTableRow> {
    val q = searchQuery.trim().lowercase()
    val out = mutableListOf<DictionaryTableRow>()
    val sorted = collections.sortedBy { it.title.lowercase() }
    for (collection in sorted) {
        val cards = collection.cards
            .filter { card -> q.isEmpty() || matchesSearch(card, q) }
            .sortedBy { it.pt.lowercase() }
        if (cards.isEmpty()) {
            if (q.isNotEmpty()) continue
            if (collection.sourceType != "manual") continue
        }
        out += DictionaryTableRow.Group(collection)
        val expanded = collection.id !in collapsedIds
        if (expanded) {
            cards.forEachIndexed { i, card ->
                out += DictionaryTableRow.WordRow(i + 1, collection, card)
            }
            if (q.isEmpty() && includeNewWords) {
                out += DictionaryTableRow.NewWord(collection)
            }
        }
    }
    return out
}

private fun matchesSearch(card: WordCard, q: String): Boolean {
    return card.pt.lowercase().contains(q) ||
        card.ru.lowercase().contains(q) ||
        card.example.orEmpty().lowercase().contains(q) ||
        card.exampleTranslation.orEmpty().lowercase().contains(q)
}

private fun sourceTypeLabel(sourceType: String?, strings: com.profconq.app.ui.i18n.UiStrings): String = when (sourceType) {
    "youtube" -> strings.sourceVideo
    "reader" -> strings.sourceBook
    "manual" -> strings.sourceManual
    else -> ""
}

@Composable
private fun searchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PpAccent,
    unfocusedBorderColor = PpBorder,
    focusedContainerColor = PpSurfaceInput,
    unfocusedContainerColor = PpSurfaceInput,
    cursorColor = PpAccent,
    focusedTextColor = PpText,
    unfocusedTextColor = PpText,
)

private fun shareDictionaryWords(context: Context, cards: List<WordCard>, title: String) {
    if (cards.isEmpty()) return
    val text = cards.joinToString("\n") { card ->
        listOf(card.pt, card.ru, card.example.orEmpty()).joinToString("\t")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
