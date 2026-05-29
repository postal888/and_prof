package com.proficon.app.ui.screens.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import com.proficon.app.data.model.Collection
import com.proficon.app.data.model.WordCard
import com.proficon.app.media.AudioRecorder
import com.proficon.app.media.playAudioFile
import com.proficon.app.media.rememberRecordAudioPermission
import com.proficon.app.media.showAudioExportChooser
import com.proficon.app.ui.components.CompactAudioPlayerBar
import com.proficon.app.ui.components.MutedText
import com.proficon.app.ui.components.ProficonBrandHeader
import com.proficon.app.ui.components.portScreenBackground
import com.proficon.app.ui.i18n.LocalUiStrings
import com.proficon.app.ui.theme.PpAccent
import com.proficon.app.ui.theme.PpBorder
import com.proficon.app.ui.theme.PpHeading
import com.proficon.app.ui.theme.PpSurface
import com.proficon.app.ui.theme.PpSurfaceInput
import com.proficon.app.ui.theme.PpText
import com.proficon.app.ui.theme.PpTextMuted

private val TableMinWidth = 920.dp

private enum class DictionaryLearnMark(val storage: String, val label: String) {
    Weak("weak", "Слабо"),
    Medium("medium", "Средне"),
    Good("good", "Хорошо"),
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

@Composable
fun DictionaryScreen(
    collections: List<Collection>,
    onCreateCollection: (title: String, description: String?, onCreated: (String) -> Unit) -> Unit,
    onUpdateCollectionTitle: (String, String) -> Unit,
    onUpdateCollectionDescription: (String, String?) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onUpdateFolderAudio: (String, String?) -> Unit,
    onAddCard: (collectionId: String, pt: String, ru: String, example: String?) -> Unit,
    onUpdateCard: (WordCard) -> Unit,
    onDeleteCard: (String) -> Unit,
    onSetLearnMark: (String, String) -> Unit,
    onOpenCardEditor: (collectionId: String, cardId: String) -> Unit,
    onBack: (() -> Unit)? = null,
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

    val tableRows = remember(filteredCollections, searchQuery) {
        buildDictionaryTableRows(filteredCollections, searchQuery)
    }

    val wordCount = tableRows.count { it is DictionaryTableRow.WordRow }

    val context = LocalContext.current
    val audioPermission = rememberRecordAudioPermission()
    val recorder = remember { AudioRecorder(context) }
    var recordingTarget by remember { mutableStateOf<DictionaryRecordingTarget?>(null) }

    fun finishRecording() {
        if (!recorder.isRecording) {
            recordingTarget = null
            return
        }
        val path = recorder.stop()
        when (val target = recordingTarget) {
            is DictionaryRecordingTarget.Folder -> onUpdateFolderAudio(target.collectionId, path)
            is DictionaryRecordingTarget.Word -> onUpdateCard(target.card.copy(audioPath = path))
            null -> Unit
        }
        recordingTarget = null
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
            } catch (_: Exception) {
                recordingTarget = null
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
        ProficonBrandHeader(
            title = strings.tabDictionary,
            subtitle = strings.dictionarySubtitle(wordCount),
            logoSize = 40.dp,
        )

        DictionaryActionBar(
            filterCollectionId = filterCollectionId,
            onCreateFolder = { folderDialog = FolderDialogState.Create },
            onAddWord = {
                val id = filterCollectionId ?: collections.firstOrNull()?.id
                if (id != null) addWordCollectionId = id
            },
            addWordEnabled = filterCollectionId != null || collections.size == 1,
        )

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
                onToggleFolderRecording = ::toggleFolderRecording,
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
                                is DictionaryTableRow.Group -> DictionaryGroupRow(
                                    collection = row.collection,
                                    horizontalScrollState = hScroll,
                                    onEdit = { folderDialog = FolderDialogState.Edit(row.collection) },
                                    onDelete = { pendingDeleteFolder = row.collection },
                                    onAddWord = { addWordCollectionId = row.collection.id },
                                )
                                is DictionaryTableRow.WordRow -> DictionaryWordTableRow(
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
                                )
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
    }

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

    addWordCollectionId?.let { collectionId ->
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

    pendingDeleteFolder?.let { collection ->
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

    pendingDelete?.let { card ->
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
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onCreateFolder,
            colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = PpHeading, modifier = Modifier.size(18.dp))
            Text(strings.dictionaryNewFolder, color = PpHeading, modifier = Modifier.padding(start = 6.dp))
        }
        Button(
            onClick = onAddWord,
            enabled = addWordEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = PpSurfaceInput,
                disabledContainerColor = PpSurfaceInput.copy(alpha = 0.5f),
            ),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = PpHeading, modifier = Modifier.size(18.dp))
            Text(strings.dictionaryAddWord, color = PpHeading, modifier = Modifier.padding(start = 6.dp))
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
    onToggleFolderRecording: (String) -> Unit,
) {
    val strings = LocalUiStrings.current
    val context = LocalContext.current
    var collectionMenuExpanded by remember { mutableStateOf(false) }
    var editingFolderTitle by rememberSaveable { mutableStateOf(false) }
    var titleDraft by rememberSaveable { mutableStateOf("") }

    val activeCollection = filterCollectionId?.let { id -> collections.find { it.id == id } }

    LaunchedEffect(activeCollection?.id, activeCollection?.title) {
        titleDraft = activeCollection?.title.orEmpty()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Поиск по PT, RU, примеру…", color = PpTextMuted) },
            shape = RoundedCornerShape(10.dp),
            colors = searchFieldColors(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExposedDropdownMenuBox(
                expanded = collectionMenuExpanded,
                onExpandedChange = { collectionMenuExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = activeCollection?.title ?: strings.dictionaryAllFolders,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collectionMenuExpanded) },
                    singleLine = true,
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
        }

        if (activeCollection != null) {
            if (editingFolderTitle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = searchFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                    )
                    Button(
                        onClick = {
                            onUpdateCollectionTitle(activeCollection.id, titleDraft)
                            editingFolderTitle = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
                    ) {
                        Text("OK", color = PpHeading)
                    }
                    Button(onClick = { editingFolderTitle = false }) {
                        Text("Отмена", color = PpText)
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { onEditFolder(activeCollection) },
                        colors = ButtonDefaults.buttonColors(containerColor = PpSurfaceInput),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(strings.dictionaryEditFolderTitle, color = PpText, modifier = Modifier.padding(start = 6.dp))
                    }
                    Button(
                        onClick = { onDeleteFolder(activeCollection) },
                        colors = ButtonDefaults.buttonColors(containerColor = PpSurfaceInput),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(strings.delete, color = PpText, modifier = Modifier.padding(start = 6.dp))
                    }
                    val isRecording = isFolderRecording(activeCollection.id)
                    Button(
                        onClick = { onToggleFolderRecording(activeCollection.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) PpAccent else PpSurfaceInput,
                        ),
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            if (isRecording) "Стоп" else "Запись папки",
                            color = PpText,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                FolderRecordingPlayerSection(
                    isRecording = isFolderRecording(activeCollection.id),
                    folderAudioPath = activeCollection.folderAudioPath,
                    onShare = { path -> showAudioExportChooser(context, path) },
                )
            }
        } else if (filteredCollections.size == 1) {
            val only = filteredCollections.first()
            val isRecording = isFolderRecording(only.id)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onToggleFolderRecording(only.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) PpAccent else PpSurfaceInput,
                        ),
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            if (isRecording) "Стоп" else "Запись папки",
                            color = PpText,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                FolderRecordingPlayerSection(
                    isRecording = isRecording,
                    folderAudioPath = only.folderAudioPath,
                    onShare = { path -> showAudioExportChooser(context, path) },
                )
            }
        }
    }
}

@Composable
private fun FolderRecordingPlayerSection(
    isRecording: Boolean,
    folderAudioPath: String?,
    onShare: (String) -> Unit,
) {
    when {
        isRecording -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(PpAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = PpAccent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Идёт запись… нажмите «Стоп», чтобы сохранить",
                    style = MaterialTheme.typography.bodySmall,
                    color = PpText,
                )
            }
        }
        !folderAudioPath.isNullOrBlank() -> {
            CompactAudioPlayerBar(
                audioPath = folderAudioPath,
                title = "Запись папки",
                onShare = { onShare(folderAudioPath) },
            )
        }
    }
}

@Composable
private fun DictionaryTableHeader(horizontalScrollState: androidx.compose.foundation.ScrollState) {
    Row(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
            .widthIn(min = TableMinWidth)
            .background(PpSurfaceInput)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableHeaderCell("№", 36.dp)
        TableHeaderCell("Португальский", 140.dp)
        TableHeaderCell("Перевод", 140.dp)
        TableHeaderCell("Источник", 72.dp)
        TableHeaderCell("Пример", 200.dp)
        TableHeaderCell("", 168.dp)
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
            val source = sourceTypeLabel(collection.sourceType)
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
            text = sourceTypeLabel(collection.sourceType),
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
                .width(168.dp)
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
    onUpdateCard: (WordCard) -> Unit,
    onDelete: () -> Unit,
    onSetLearnMark: (String, String) -> Unit,
    onOpenCardEditor: () -> Unit,
) {
    val context = LocalContext.current
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
            text = sourceTypeLabel(collection.sourceType),
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
                .width(168.dp)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LearnMarkDropdown(
                card = card,
                onSetLearnMark = { onSetLearnMark(card.id, it) },
            )
            if (!card.audioPath.isNullOrBlank()) {
                IconButton(
                    onClick = { playAudioFile(card.audioPath!!) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Прослушать", tint = PpAccent, modifier = Modifier.size(18.dp))
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
            card.audioPath?.let { path ->
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
    var expanded by remember { mutableStateOf(false) }
    val current = learnMarkForCard(card)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.width(88.dp),
    ) {
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .heightIn(max = 40.dp),
            textStyle = TextStyle(fontSize = 10.sp, color = PpText),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            colors = searchFieldColors(),
            shape = RoundedCornerShape(6.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = PpSurface,
        ) {
            DictionaryLearnMark.entries.forEach { mark ->
                DropdownMenuItem(
                    text = { Text(mark.label, color = PpText, fontSize = 12.sp) },
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

private fun buildDictionaryTableRows(
    collections: List<Collection>,
    searchQuery: String,
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
        if (q.isEmpty()) {
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

private fun sourceTypeLabel(sourceType: String?): String = when (sourceType) {
    "youtube" -> "Видео"
    "reader" -> "Книга"
    "manual" -> "Вручную"
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
