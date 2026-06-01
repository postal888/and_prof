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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.profconq.app.data.model.Collection
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
import com.profconq.app.ui.components.CompactAudioPlayerBar
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.i18n.LocalStudyLanguagePrefs
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpBorder
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.ui.study.StudyWordSelectionStore

private val TableMinWidth = 1040.dp
private val TableActionsWidth = 288.dp

private enum class DictionaryLearnMark(val storage: String) {
    Weak("weak"),
    Medium("medium"),
    Good("good"),
    ;

    fun label(strings: com.profconq.app.ui.i18n.UiStrings): String = when (this) {
        Weak -> strings.dictionaryMasteryWeak
        Medium -> strings.dictionaryMasteryMedium
        Good -> strings.dictionaryMasteryGood
    }
}

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

    val filteredCollections = remember(collections, filterCollectionId) {
        if (filterCollectionId == null) collections
        else collections.filter { it.id == filterCollectionId }
    }

    val tableRows = remember(filteredCollections, searchQuery, selectionMode) {
        buildDictionaryTableRows(filteredCollections, searchQuery, includeNewWords = !selectionMode)
    }

    val selectedIdsFlow = selectionStore?.selectedIds
    val selectedIds by if (selectedIdsFlow != null) {
        selectedIdsFlow.collectAsState()
    } else {
        remember { mutableStateOf(emptySet<String>()) }
    }

    val wordCount = tableRows.count { it is DictionaryTableRow.WordRow }

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
            .padding(horizontal = 14.dp),
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = PpAccent)
                }
            }
        }
        TabScreenHeader(
            tab = MainTab.Dictionary,
            subtitle = strings.dictionarySubtitle(wordCount),
        )

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
            Button(
                onClick = { folderDialog = FolderDialogState.Create },
                colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = PpHeading)
                Text(
                    strings.dictionaryNewFolder,
                    color = PpHeading,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
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

            if (tableRows.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                MutedText(strings.dictionaryNoSearchResults)
            } else {
                val hScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, PpBorder, RoundedCornerShape(12.dp))
                        .background(PpSurface),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item(key = "header") {
                            DictionaryTableHeader(horizontalScrollState = hScroll)
                        }
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
                                    if (selectionMode && selectionStore != null) {
                                        DictionarySelectionGroupRow(
                                            collection = row.collection,
                                            cardIds = folderCardIds,
                                            selectedIds = selectedIds,
                                            onToggleFolder = { selectAll ->
                                                selectionStore.setFolder(folderCardIds, selectAll)
                                            },
                                        )
                                    } else {
                                        DictionaryGroupRow(
                                            collection = row.collection,
                                            horizontalScrollState = hScroll,
                                            onEdit = { folderDialog = FolderDialogState.Edit(row.collection) },
                                            onDelete = { pendingDeleteFolder = row.collection },
                                            onAddWord = { addWordCollectionId = row.collection.id },
                                        )
                                    }
                                }
                                is DictionaryTableRow.WordRow -> {
                                    if (selectionMode && selectionStore != null) {
                                        DictionarySelectionWordRow(
                                            card = row.card,
                                            checked = row.card.id in selectedIds,
                                            onToggle = { selectionStore.toggle(row.card.id) },
                                        )
                                    } else {
                                        DictionaryWordTableRow(
                                            index = row.index,
                                            collection = row.collection,
                                            card = row.card,
                                            horizontalScrollState = hScroll,
                                            isRecording = isWordRecording(row.card.id),
                                            onToggleRecording = { toggleWordRecording(row.card) },
                                            onUpdateCard = onUpdateCard,
                                            onDelete = { pendingDelete = row.card },
                                            onSetLearnMark = onSetLearnMark,
                                            onOpenCardEditor = { onOpenCardEditor(row.collection.id, row.card.id) },
                                            onTrimAudio = { path ->
                                                audioTrimTarget = DictionaryAudioTrimTarget.Word(
                                                    row.card,
                                                    path,
                                                    fresh = false,
                                                )
                                            },
                                        )
                                    }
                                }
                                is DictionaryTableRow.NewWord -> DictionaryNewWordRow(
                                    collection = row.collection,
                                    horizontalScrollState = hScroll,
                                    onAdd = { pt, ru, ex ->
                                        onAddCard(row.collection.id, pt, ru, ex)
                                    },
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
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
        Button(
            onClick = onCreateFolder,
            colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = PpHeading, modifier = Modifier.size(17.dp))
            Text(
                text = strings.dictionaryNewFolder,
                color = PpHeading,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Button(
            onClick = onAddWord,
            enabled = addWordEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = PpSurfaceInput,
                disabledContainerColor = PpSurfaceInput.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = PpHeading, modifier = Modifier.size(17.dp))
            Text(
                text = strings.dictionaryAddWord,
                color = PpHeading,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
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
                            "Поиск по PT, RU, примеру…",
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
                                colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text("OK", color = PpHeading, style = MaterialTheme.typography.labelLarge)
                            }
                            Button(
                                onClick = { editingFolderTitle = false },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text("Отмена", color = PpText, style = MaterialTheme.typography.labelLarge)
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
                        onShare = { path -> showAudioExportChooser(context, path) },
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
            text = if (isRecording) "Стоп" else "Запись папки",
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
                    text = "Идёт запись",
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
private fun DictionaryTableHeader(horizontalScrollState: androidx.compose.foundation.ScrollState) {
    val strings = LocalUiStrings.current
    val studyLangs = LocalStudyLanguagePrefs.current
    Row(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
            .widthIn(min = TableMinWidth)
            .background(PpSurfaceInput)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableHeaderCell("№", 36.dp)
        TableHeaderCell(strings.studyLanguageName(studyLangs.source), 140.dp)
        TableHeaderCell(strings.studyLanguageName(studyLangs.target), 140.dp)
        TableHeaderCell(strings.dictionarySourceColumn, 72.dp)
        TableHeaderCell(strings.dictionaryWordExampleLabel, 200.dp)
        TableHeaderCell("", TableActionsWidth)
    }
}

@Composable
private fun TableHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text.uppercase(),
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = PpTextMuted,
        letterSpacing = 0.8.sp,
        fontSize = 10.sp,
    )
}

@Composable
private fun DictionaryGroupRow(
    collection: Collection,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddWord: () -> Unit,
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
            .widthIn(min = TableMinWidth)
            .fillMaxWidth()
            .background(PpAccent.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${collection.title} · ${collection.cards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = PpHeading,
                fontWeight = FontWeight.SemiBold,
            )
            val source = sourceTypeLabel(collection.sourceType, strings)
            if (source.isNotEmpty()) {
                Text(
                    text = " · $source",
                    style = MaterialTheme.typography.labelMedium,
                    color = PpTextMuted,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
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
private fun DictionaryNewWordRow(
    collection: Collection,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onAdd: (pt: String, ru: String, example: String?) -> Unit,
) {
    val strings = LocalUiStrings.current
    var pt by rememberSaveable(collection.id) { mutableStateOf("") }
    var ru by rememberSaveable(collection.id) { mutableStateOf("") }
    var example by rememberSaveable(collection.id) { mutableStateOf("") }

    Row(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
            .widthIn(min = TableMinWidth)
            .fillMaxWidth()
            .background(PpSurfaceInput.copy(alpha = 0.25f))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+",
            modifier = Modifier
                .width(36.dp)
                .padding(horizontal = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = PpAccent,
            fontWeight = FontWeight.Bold,
        )
        DictionaryEditableCell(
            value = pt,
            width = 140.dp,
            singleLine = true,
            fontWeight = FontWeight.SemiBold,
            onValueChange = { pt = it },
            onCommit = { pt = it },
        )
        DictionaryEditableCell(
            value = ru,
            width = 140.dp,
            singleLine = true,
            onValueChange = { ru = it },
            onCommit = { ru = it },
        )
        Text(
            text = sourceTypeLabel(collection.sourceType, strings),
            modifier = Modifier
                .width(72.dp)
                .padding(horizontal = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = PpTextMuted,
            fontSize = 11.sp,
        )
        DictionaryEditableCell(
            value = example,
            width = 200.dp,
            singleLine = false,
            minLines = 1,
            onValueChange = { example = it },
            onCommit = { example = it },
        )
        Button(
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
            enabled = pt.trim().isNotEmpty() && ru.trim().isNotEmpty(),
            modifier = Modifier
                .width(TableActionsWidth)
                .padding(end = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
        ) {
            Text(strings.dictionaryAddWord, color = PpHeading, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DictionaryWordTableRow(
    index: Int,
    collection: Collection,
    card: WordCard,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    onTrimAudio: (String) -> Unit,
    onUpdateCard: (WordCard) -> Unit,
    onDelete: () -> Unit,
    onSetLearnMark: (String, String) -> Unit,
    onOpenCardEditor: () -> Unit,
) {
    val context = LocalContext.current
    val strings = LocalUiStrings.current
    val rowBg = learnMarkRowColor(card)

    Row(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
            .widthIn(min = TableMinWidth)
            .fillMaxWidth()
            .background(rowBg)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString(),
            modifier = Modifier
                .width(36.dp)
                .padding(horizontal = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = PpTextMuted,
        )
        DictionaryEditableCell(
            value = card.pt,
            width = 140.dp,
            fontWeight = FontWeight.SemiBold,
            singleLine = true,
            onCommit = { newPt ->
                val trimmed = newPt.trim()
                if (trimmed.isNotEmpty() && trimmed != card.pt) {
                    onUpdateCard(card.copy(pt = trimmed))
                }
            },
        )
        DictionaryEditableCell(
            value = card.ru,
            width = 140.dp,
            singleLine = true,
            onCommit = { newRu ->
                val trimmed = newRu.trim()
                if (trimmed.isNotEmpty() && trimmed != card.ru) {
                    onUpdateCard(card.copy(ru = trimmed))
                }
            },
        )
        Text(
            text = sourceTypeLabel(collection.sourceType, strings),
            modifier = Modifier
                .width(72.dp)
                .padding(horizontal = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = PpTextMuted,
            fontSize = 11.sp,
        )
        DictionaryEditableCell(
            value = card.example.orEmpty(),
            width = 200.dp,
            singleLine = false,
            minLines = 2,
            onCommit = { newEx ->
                val trimmed = newEx.trim()
                val normalized = trimmed.ifEmpty { null }
                if (normalized != card.example) {
                    onUpdateCard(card.copy(example = normalized))
                }
            },
        )
        Row(
            modifier = Modifier
                .width(TableActionsWidth)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LearnMarkDropdown(
                card = card,
                onSetLearnMark = { onSetLearnMark(card.id, it) },
            )
            val firstAudio = card.resolvedAudioPaths().firstOrNull()
            if (firstAudio != null) {
                IconButton(
                    onClick = { playAudioFile(firstAudio) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Прослушать", tint = PpAccent, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { onTrimAudio(firstAudio) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.ContentCut,
                        contentDescription = strings.audioTrimEdit,
                        tint = PpTextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            IconButton(
                onClick = onToggleRecording,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = if (isRecording) "Стоп" else "Запись",
                    tint = if (isRecording) PpAccent else PpTextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            firstAudio?.let { path ->
                IconButton(
                    onClick = { showAudioExportChooser(context, path) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Экспорт", tint = PpTextMuted, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onOpenCardEditor, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Редактор", tint = PpTextMuted, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = PpTextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearnMarkDropdown(
    card: WordCard,
    onSetLearnMark: (String) -> Unit,
) {
    val strings = LocalUiStrings.current
    var expanded by remember { mutableStateOf(false) }
    val current = learnMarkForCard(card)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.widthIn(min = 96.dp),
    ) {
        Row(
            modifier = Modifier
                .menuAnchor()
                .height(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, PpBorder, RoundedCornerShape(6.dp))
                .background(PpSurfaceInput)
                .clickable { expanded = !expanded }
                .padding(start = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = current.label(strings),
                style = MaterialTheme.typography.labelSmall,
                color = PpText,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PpTextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = PpSurface,
        ) {
            DictionaryLearnMark.entries.forEach { mark ->
                DropdownMenuItem(
                    text = { Text(mark.label(strings), color = PpText, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onSetLearnMark(mark.storage)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DictionaryEditableCell(
    value: String,
    width: androidx.compose.ui.unit.Dp,
    singleLine: Boolean,
    minLines: Int = 1,
    fontWeight: FontWeight? = null,
    onValueChange: ((String) -> Unit)? = null,
    onCommit: (String) -> Unit,
) {
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    LaunchedEffect(value) { draft = value }

    BasicTextField(
        value = draft,
        onValueChange = {
            draft = it
            onValueChange?.invoke(it)
        },
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(PpSurfaceInput.copy(alpha = 0.35f))
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .onFocusChanged { focus ->
                if (!focus.isFocused && draft != value) {
                    onCommit(draft)
                }
            },
        textStyle = TextStyle(
            color = PpText,
            fontSize = 13.sp,
            fontWeight = fontWeight ?: FontWeight.Normal,
            lineHeight = 18.sp,
        ),
        cursorBrush = SolidColor(PpAccent),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = if (singleLine) 1 else 4,
    )
}

@Composable
private fun SelectionModeBanner(selectedCount: Int) {
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
            text = "Выберите слова для набора · Выбрано: $selectedCount",
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = PpSurfaceInput),
        ) {
            Text("Отмена", color = PpText)
        }
        Button(
            onClick = onDone,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
        ) {
            Text("Готово ($selectedCount)", color = PpHeading)
        }
    }
}

@Composable
private fun DictionarySelectionGroupRow(
    collection: Collection,
    cardIds: List<String>,
    selectedIds: Set<String>,
    onToggleFolder: (selectAll: Boolean) -> Unit,
) {
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
            .background(PpAccent.copy(alpha = 0.12f))
            .clickable {
                onToggleFolder(toggleState != ToggleableState.On)
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = toggleState,
            onClick = { onToggleFolder(toggleState != ToggleableState.On) },
            colors = CheckboxDefaults.colors(checkedColor = PpAccent),
        )
        Text(
            text = "${collection.title} · ${collection.cards.size}",
            style = MaterialTheme.typography.labelMedium,
            color = PpHeading,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun DictionarySelectionWordRow(
    card: WordCard,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = PpAccent),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(card.pt, color = PpHeading, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(card.ru, color = PpTextMuted, style = MaterialTheme.typography.bodyMedium)
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
): List<DictionaryTableRow> {
    val q = searchQuery.trim().lowercase()
    val out = mutableListOf<DictionaryTableRow>()
    var index = 0
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
        for (card in cards) {
            index++
            out += DictionaryTableRow.WordRow(index, collection, card)
        }
        if (q.isEmpty() && includeNewWords) {
            out += DictionaryTableRow.NewWord(collection)
        }
    }
    return out
}

private fun matchesSearch(card: WordCard, q: String): Boolean {
    return card.pt.lowercase().contains(q) ||
        card.ru.lowercase().contains(q) ||
        card.example.orEmpty().lowercase().contains(q)
}

private fun learnMarkForCard(card: WordCard): DictionaryLearnMark = when {
    card.known -> DictionaryLearnMark.Good
    card.due -> DictionaryLearnMark.Weak
    else -> DictionaryLearnMark.Medium
}

@Composable
private fun learnMarkRowColor(card: WordCard): androidx.compose.ui.graphics.Color = when (learnMarkForCard(card)) {
    DictionaryLearnMark.Good -> PpAccent.copy(alpha = 0.08f)
    DictionaryLearnMark.Medium -> androidx.compose.ui.graphics.Color(0xFFE8C547).copy(alpha = 0.12f)
    DictionaryLearnMark.Weak -> androidx.compose.ui.graphics.Color(0xFFE85C5C).copy(alpha = 0.10f)
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
