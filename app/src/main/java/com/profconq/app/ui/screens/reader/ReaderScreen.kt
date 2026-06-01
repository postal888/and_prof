package com.profconq.app.ui.screens.reader

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.profconq.app.ui.theme.PpDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profconq.app.data.model.SubtitleCursorMode
import com.profconq.app.data.model.ReaderLineSpacing
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.reader.ReaderDemoBooks
import com.profconq.app.reader.ReaderImportParser
import com.profconq.app.reader.ReaderFontSize
import com.profconq.app.reader.ReaderPaging
import com.profconq.app.reader.ReaderViewModel
import com.profconq.app.reader.ReaderViewModelFactory
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.profconq.app.ui.components.MutedText
import com.profconq.app.ui.components.PortCard
import com.profconq.app.ui.components.ProfconqLogo
import com.profconq.app.ui.components.TabScreenHeader
import com.profconq.app.ui.navigation.MainTab
import com.profconq.app.ui.i18n.LocalUiStrings
import com.profconq.app.ui.components.SectionTitle
import com.profconq.app.ui.components.translation.CursorModeToggle
import com.profconq.app.ui.components.translation.InteractiveParagraph
import com.profconq.app.ui.components.translation.PhrasePreviewBar
import com.profconq.app.ui.components.translation.ReadingWordToken
import com.profconq.app.ui.components.translation.WordTranslationBar
import com.profconq.app.ui.components.translation.readingTextStyle
import com.profconq.app.ui.theme.PpAccent
import com.profconq.app.ui.theme.PpAccentGlow
import com.profconq.app.ui.theme.PpAccentSoft
import com.profconq.app.ui.components.portScreenBackground
import com.profconq.app.ui.theme.PpBg
import com.profconq.app.ui.theme.PpBrandNavy
import com.profconq.app.ui.theme.PpDanger
import com.profconq.app.ui.theme.PpGold
import com.profconq.app.ui.theme.PpHeading
import com.profconq.app.ui.theme.PpSurface
import com.profconq.app.ui.theme.PpSurfaceInput
import com.profconq.app.ui.theme.PpText
import com.profconq.app.ui.theme.PpTextMuted
import com.profconq.app.youtube.SubtitlePhraseExtractor

@Composable
fun ReaderScreen(
    repository: ProfconqRepository,
    authTokenProvider: suspend (Boolean) -> String? = { null },
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel(
        factory = ReaderViewModelFactory(repository, authTokenProvider),
    ),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val strings = LocalUiStrings.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val content = ReaderImportParser.readContent(
                uri = uri,
                openStream = { context.contentResolver.openInputStream(it) },
                mimeType = { context.contentResolver.getType(it) },
            )
            val title = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                ?.replace('_', ' ')
                ?.trim()
                .orEmpty()
                .ifBlank { strings.readerImportedBookTitle }
            viewModel.importBook(title, content, uri.toString())
        }.onFailure { error ->
            viewModel.showError(error.message ?: strings.readerImportFailed)
        }
    }

    LaunchedEffect(state.addToast) {
        state.addToast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddToast()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PpBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = PpSurfaceInput,
                    contentColor = PpHeading,
                    actionColor = PpAccent,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
    ) { padding ->
        if (state.activeBookId == null) {
            ReaderLibrary(
                state = state,
                onOpenBook = viewModel::openBook,
                onDeleteBook = viewModel::deleteBook,
                onImport = {
                    importLauncher.launch(
                        arrayOf(
                            "text/plain",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        ),
                    )
                },
                modifier = Modifier.padding(padding),
            )
        } else {
            ReaderBookView(
                state = state,
                onBack = viewModel::closeBook,
                onScrollParagraph = viewModel::updateScrollParagraph,
                onJumpToPage = viewModel::jumpToPage,
                onJumpConsumed = viewModel::consumePendingJump,
                onToggleBookmark = viewModel::toggleBookmark,
                onGoToBookmark = viewModel::goToBookmark,
                onDismissBookmarkFeedback = viewModel::clearBookmarkFeedback,
                onCursorModeChange = viewModel::setCursorMode,
                onFontSizeChange = viewModel::setReaderFontSizeLevel,
                onLineSpacingChange = viewModel::setReaderLineSpacingPercent,
                onWordClick = { token, paragraph ->
                    val example = if (state.wordContextExampleEnabled) {
                        SubtitlePhraseExtractor.extractAroundWord(
                            paragraph,
                            token.start,
                            token.end,
                        )
                    } else {
                        null
                    }
                    viewModel.onWordClicked(token.cleanPt, example)
                },
                onPhraseSelected = { phrase -> viewModel.onPhraseSelected(phrase, phrase) },
                onAddWord = viewModel::addSelectedWordToDictionary,
                onDismissWord = viewModel::clearSelectedWord,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = padding.calculateTopPadding(),
                        start = padding.calculateStartPadding(LayoutDirection.Ltr),
                        end = padding.calculateEndPadding(LayoutDirection.Ltr),
                    ),
            )
        }
    }
}

@Composable
private fun ReaderLibrary(
    state: com.profconq.app.reader.ReaderUiState,
    onOpenBook: (String) -> Unit,
    onDeleteBook: (String) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalUiStrings.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            TabScreenHeader(tab = MainTab.Reader, subtitle = strings.readerScreenSubtitle)
        }

        item {
            Button(
                onClick = onImport,
                enabled = !state.isImporting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PpAccent),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        color = PpHeading,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = PpHeading)
                        Text(strings.readerImportButton, color = PpHeading)
                    }
                }
            }
        }

        item {
            SectionTitle(title = strings.readerLibrarySection)
        }

        if (state.books.isEmpty()) {
            item {
                PortCard {
                    MutedText(strings.readerNoBooks)
                }
            }
        } else {
            items(state.books, key = { it.id }) { book ->
                BookRow(
                    title = book.title,
                    preview = book.content.lineSequence().firstOrNull().orEmpty(),
                    canDelete = book.id != ReaderDemoBooks.DEMO_ID,
                    onOpen = { onOpenBook(book.id) },
                    onDelete = { onDeleteBook(book.id) },
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun BookRow(
    title: String,
    preview: String,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    PortCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PpHeading,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (preview.isNotBlank()) {
                    MutedText(
                        text = preview,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = PpDanger)
                }
            }
        }
    }
}

@Composable
private fun ReaderTypographyPanel(
    fontLevel: Int,
    lineSpacingPercent: Int,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Int) -> Unit,
) {
    val clampedFont = fontLevel.coerceIn(ReaderFontSize.MIN_LEVEL, ReaderFontSize.MAX_LEVEL)
    val clampedSpacing = lineSpacingPercent.coerceIn(ReaderLineSpacing.MIN_PERCENT, ReaderLineSpacing.MAX_PERCENT)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PpSurfaceInput)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Размер шрифта: ${ReaderFontSize.labelForLevel(clampedFont)}",
            color = PpText,
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = clampedFont.toFloat(),
            valueRange = ReaderFontSize.MIN_LEVEL.toFloat()..ReaderFontSize.MAX_LEVEL.toFloat(),
            steps = ReaderFontSize.MAX_LEVEL - ReaderFontSize.MIN_LEVEL - 1,
            onValueChange = { onFontSizeChange(it.toInt()) },
        )

        Text(
            text = "Межстрочный интервал: ${clampedSpacing}%",
            color = PpText,
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = clampedSpacing.toFloat(),
            valueRange = ReaderLineSpacing.MIN_PERCENT.toFloat()..ReaderLineSpacing.MAX_PERCENT.toFloat(),
            steps = ((ReaderLineSpacing.MAX_PERCENT - ReaderLineSpacing.MIN_PERCENT) / 5).coerceAtLeast(1),
            onValueChange = { onLineSpacingChange(it.toInt()) },
        )
    }
}

@Composable
private fun ReaderBookmarkNotice(
    pageNumber: Int?,
    modifier: Modifier = Modifier,
) {
    val isSet = pageNumber != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        PpAccentSoft.copy(alpha = 0.95f),
                        PpSurfaceInput,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = PpAccent.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PpAccent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = if (isSet) PpGold else PpTextMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isSet) "Закладка сохранена" else "Закладка снята",
                color = PpHeading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isSet) {
                    "Страница $pageNumber · долгое нажатие на ★ — перейти"
                } else {
                    "Можно поставить снова на текущей странице"
                },
                color = PpText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    hasBookmark: Boolean,
    bookmarkOnCurrentPage: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageIndicatorClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onGoToBookmark: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PpSurface),
    ) {
        HorizontalDivider(color = PpDivider, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousPage, enabled = currentPage > 0) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Предыдущая страница",
                    tint = if (currentPage > 0) PpAccent else PpTextMuted,
                )
            }

            TextButton(onClick = onPageIndicatorClick) {
                Text(
                    text = "${currentPage + 1} / $totalPages",
                    color = PpHeading,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            IconButton(onClick = onNextPage, enabled = currentPage < totalPages - 1) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Следующая страница",
                    tint = if (currentPage < totalPages - 1) PpAccent else PpTextMuted,
                )
            }

            val bookmarkInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            bookmarkOnCurrentPage -> PpAccent.copy(alpha = 0.28f)
                            hasBookmark -> PpAccentGlow
                            else -> Color.Transparent
                        },
                    )
                    .combinedClickable(
                        interactionSource = bookmarkInteraction,
                        indication = null,
                        onClick = onToggleBookmark,
                        onLongClick = { if (hasBookmark) onGoToBookmark() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (bookmarkOnCurrentPage || hasBookmark) {
                        Icons.Default.Bookmark
                    } else {
                        Icons.Default.BookmarkBorder
                    },
                    contentDescription = if (hasBookmark) {
                        "Закладка (долгое нажатие — перейти)"
                    } else {
                        "Поставить закладку"
                    },
                    tint = when {
                        bookmarkOnCurrentPage -> PpGold
                        hasBookmark -> PpAccent
                        else -> PpTextMuted
                    },
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ReaderPageJumpDialog(
    totalPages: Int,
    initialPage: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var pageInput by remember { mutableStateOf((initialPage + 1).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Перейти на страницу", color = PpHeading) },
        text = {
            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it.filter { ch -> ch.isDigit() }.take(6) },
                label = { Text("Страница (1–$totalPages)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val page = pageInput.toIntOrNull()?.coerceIn(1, totalPages) ?: return@TextButton
                    onConfirm(page - 1)
                    onDismiss()
                },
            ) {
                Text("Перейти", color = PpAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = PpTextMuted)
            }
        },
        containerColor = PpSurface,
    )
}

@Composable
private fun ReaderBookView(
    state: com.profconq.app.reader.ReaderUiState,
    onBack: () -> Unit,
    onScrollParagraph: (Int) -> Unit,
    onJumpToPage: (Int) -> Unit,
    onJumpConsumed: () -> Unit,
    onToggleBookmark: () -> Unit,
    onGoToBookmark: () -> Unit,
    onDismissBookmarkFeedback: () -> Unit,
    onCursorModeChange: (SubtitleCursorMode) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Int) -> Unit,
    onWordClick: (ReadingWordToken, String) -> Unit,
    onPhraseSelected: (String) -> Unit,
    onAddWord: () -> Unit,
    onDismissWord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageSize = ReaderPaging.PARAGRAPHS_PER_PAGE
    val bufferPages = ReaderPaging.BUFFER_PAGES
    val paragraphCount = state.paragraphs.size
    val totalPages = ((paragraphCount + pageSize - 1) / pageSize).coerceAtLeast(1)

    var bufferStartPage by rememberSaveable(state.activeBookId) { mutableIntStateOf(0) }
    var bufferEndPage by rememberSaveable(state.activeBookId) { mutableIntStateOf(bufferPages) }
    var currentPage by rememberSaveable(state.activeBookId) { mutableIntStateOf(0) }
    var showTypographyPanel by rememberSaveable(state.activeBookId) { mutableStateOf(false) }
    var showPageJumpDialog by remember { mutableStateOf(false) }
    var pendingPhraseSelection by remember { mutableStateOf("") }
    var selectionGestureActive by remember { mutableStateOf(false) }
    val scrollFrozen = selectionGestureActive

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val textStyle = readingTextStyle(
        fontSizeLevel = state.readerFontSizeLevel,
        lineSpacingPercent = state.readerLineSpacingPercent,
    )
    val flowRowLineGap = ReaderFontSize.flowRowLineGap(
        fontSizeLevel = state.readerFontSizeLevel,
        lineSpacingPercent = state.readerLineSpacingPercent,
    )
    val cursorMode = if (state.phraseCopyEnabled) state.cursorMode else SubtitleCursorMode.Tap

    LaunchedEffect(cursorMode) {
        if (cursorMode != SubtitleCursorMode.Select) {
            pendingPhraseSelection = ""
            selectionGestureActive = false
        }
    }

    LaunchedEffect(pendingPhraseSelection) {
        if (pendingPhraseSelection.isNotEmpty()) {
            onDismissWord()
        }
    }

    fun pageToParagraphStart(page: Int): Int = page.coerceIn(0, totalPages - 1) * pageSize

    fun applyWindowForParagraph(paragraph: Int, scrollItemInBuffer: Int) {
        val page = (paragraph / pageSize).coerceIn(0, totalPages - 1)
        bufferStartPage = page
        bufferEndPage = (page + bufferPages).coerceAtMost(totalPages - 1)
        currentPage = page
        scope.launch {
            listState.scrollToItem(scrollItemInBuffer.coerceAtLeast(0))
        }
    }

    val bufferStartParagraph = bufferStartPage * pageSize
    val bufferEndParagraph = ((bufferEndPage + 1) * pageSize).coerceAtMost(paragraphCount)
    val bufferedParagraphs = if (
        paragraphCount > 0 &&
        bufferStartParagraph < bufferEndParagraph
    ) {
        state.paragraphs.subList(bufferStartParagraph, bufferEndParagraph)
    } else {
        emptyList()
    }

    val bookmarkPage = state.bookmarkParagraph?.let { it / pageSize }
    val bookmarkOnCurrentPage = state.bookmarkParagraph != null &&
        currentPage == bookmarkPage

    LaunchedEffect(state.activeBookId, paragraphCount) {
        if (paragraphCount == 0) return@LaunchedEffect
        val target = state.scrollParagraph.coerceIn(0, paragraphCount - 1)
        val page = (target / pageSize).coerceIn(0, totalPages - 1)
        bufferStartPage = page
        bufferEndPage = (page + bufferPages).coerceAtMost(totalPages - 1)
        currentPage = page
        val itemIndex = target - pageToParagraphStart(page)
        listState.scrollToItem(itemIndex.coerceAtLeast(0))
    }

    LaunchedEffect(state.pendingJumpParagraph, paragraphCount) {
        val jump = state.pendingJumpParagraph ?: return@LaunchedEffect
        if (paragraphCount == 0) {
            onJumpConsumed()
            return@LaunchedEffect
        }
        val target = jump.coerceIn(0, paragraphCount - 1)
        val page = (target / pageSize).coerceIn(0, totalPages - 1)
        val itemIndex = target - pageToParagraphStart(page)
        applyWindowForParagraph(target, itemIndex)
        onJumpConsumed()
    }

    LaunchedEffect(listState, bufferStartParagraph, bufferedParagraphs.size) {
        if (bufferedParagraphs.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            val first = listState.firstVisibleItemIndex
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: first
            first to lastVisible
        }
            .distinctUntilChanged()
            .collect { (first, lastVisible) ->
                if (scrollFrozen) return@collect

                val globalParagraph = (bufferStartParagraph + first)
                    .coerceAtMost(paragraphCount - 1)
                onScrollParagraph(globalParagraph)
                currentPage = (globalParagraph / pageSize).coerceIn(0, totalPages - 1)

                val itemsInBuffer = bufferedParagraphs.size
                val nearEnd = lastVisible >= itemsInBuffer - 3
                if (nearEnd && bufferEndPage < totalPages - 1) {
                    bufferEndPage = (bufferEndPage + bufferPages).coerceAtMost(totalPages - 1)
                }

                val nearStart = first <= 2
                if (nearStart && bufferStartPage > 0) {
                    val newStartPage = (bufferStartPage - bufferPages).coerceAtLeast(0)
                    if (newStartPage < bufferStartPage) {
                        val addedItems = (bufferStartPage - newStartPage) * pageSize
                        bufferStartPage = newStartPage
                        scope.launch {
                            listState.scrollToItem((first + addedItems).coerceAtLeast(0))
                        }
                    }
                }
            }
    }

    if (showPageJumpDialog) {
        ReaderPageJumpDialog(
            totalPages = totalPages,
            initialPage = currentPage,
            onDismiss = { showPageJumpDialog = false },
            onConfirm = { pageIndex -> onJumpToPage(pageIndex) },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .portScreenBackground(),
    ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PpSurface)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = PpAccent)
                }
                ProfconqLogo(size = 32.dp)
                Text(
                    text = state.activeBookTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = PpHeading,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { showTypographyPanel = !showTypographyPanel }) {
                    Icon(
                        imageVector = if (showTypographyPanel) Icons.Default.KeyboardArrowUp else Icons.Default.Settings,
                        contentDescription = "Шрифт и интервал",
                        tint = PpAccent,
                    )
                }
            }

            if (showTypographyPanel) {
                ReaderTypographyPanel(
                    fontLevel = state.readerFontSizeLevel,
                    lineSpacingPercent = state.readerLineSpacingPercent,
                    onFontSizeChange = onFontSizeChange,
                    onLineSpacingChange = onLineSpacingChange,
                )
            }

            if (state.phraseCopyEnabled) {
                CursorModeToggle(
                    mode = cursorMode,
                    onModeChange = onCursorModeChange,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }

            if (
                cursorMode == SubtitleCursorMode.Select &&
                pendingPhraseSelection.isNotEmpty() &&
                state.selectedWord == null
            ) {
                PhrasePreviewBar(
                    phrase = pendingPhraseSelection,
                    textStyle = textStyle,
                    onTranslate = {
                        val phrase = pendingPhraseSelection
                        if (phrase.isNotEmpty()) {
                            onPhraseSelected(phrase)
                        }
                        pendingPhraseSelection = ""
                        selectionGestureActive = false
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }

            state.selectedWord?.let { word ->
                WordTranslationBar(
                    word = word,
                    canAddToDictionary = !word.isPhrase || state.phraseCopyEnabled,
                    onAdd = onAddWord,
                    onDismiss = onDismissWord,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    state = listState,
                    userScrollEnabled = !scrollFrozen,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    itemsIndexed(
                        bufferedParagraphs,
                        key = { index, _ ->
                            "${bufferStartParagraph + index}_${state.readerFontSizeLevel}"
                        },
                    ) { _, paragraph ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PpSurfaceInput)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            InteractiveParagraph(
                                text = paragraph,
                                cursorMode = cursorMode,
                                savedWords = state.savedWords,
                                textStyle = textStyle,
                                flowRowLineGap = flowRowLineGap,
                                onWordClick = { token -> onWordClick(token, paragraph) },
                                onPhraseSelected = { phrase ->
                                    pendingPhraseSelection = ""
                                    selectionGestureActive = false
                                    onPhraseSelected(phrase)
                                },
                                onPhraseSelectionChange = { phrase ->
                                    if (phrase.isNotEmpty()) {
                                        pendingPhraseSelection = phrase
                                    }
                                },
                                onSelectionActiveChange = { selectionGestureActive = it },
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

            }

            val bookmarkFeedback = state.bookmarkFeedback
            LaunchedEffect(bookmarkFeedback?.nonce) {
                if (bookmarkFeedback != null) {
                    delay(2_800)
                    onDismissBookmarkFeedback()
                }
            }

            AnimatedVisibility(
                visible = bookmarkFeedback != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                bookmarkFeedback?.let { feedback ->
                    ReaderBookmarkNotice(pageNumber = feedback.pageNumber)
                }
            }

            ReaderBottomBar(
            currentPage = currentPage,
            totalPages = totalPages,
            hasBookmark = state.bookmarkParagraph != null,
            bookmarkOnCurrentPage = bookmarkOnCurrentPage,
            onPreviousPage = {
                if (currentPage > 0) {
                    onJumpToPage(currentPage - 1)
                }
            },
            onNextPage = {
                if (currentPage < totalPages - 1) {
                    onJumpToPage(currentPage + 1)
                }
            },
            onPageIndicatorClick = { showPageJumpDialog = true },
            onToggleBookmark = onToggleBookmark,
            onGoToBookmark = onGoToBookmark,
        )
    }
}
