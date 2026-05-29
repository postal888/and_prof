package com.proficon.app.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proficon.app.data.model.ReaderBook
import com.proficon.app.data.model.ReaderLineSpacing
import com.proficon.app.data.model.SubtitleCursorMode
import com.proficon.app.data.repository.ProficonRepository
import com.proficon.app.data.repository.WordNormalizer
import com.proficon.app.ui.components.translation.PhraseSelectionLimits
import com.proficon.app.ui.components.translation.splitBookParagraphs
import com.proficon.app.youtube.SelectedWord
import com.proficon.app.youtube.WordTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

data class ReaderUiState(
    val books: List<ReaderBook> = emptyList(),
    val activeBookId: String? = null,
    val activeBookTitle: String? = null,
    val paragraphs: List<String> = emptyList(),
    val scrollParagraph: Int = 0,
    val bookmarkParagraph: Int? = null,
    val bookmarkFeedback: ReaderBookmarkFeedback? = null,
    /** One-shot scroll target for UI (page jump / bookmark). */
    val pendingJumpParagraph: Int? = null,
    val selectedWord: SelectedWord? = null,
    val cursorMode: SubtitleCursorMode = SubtitleCursorMode.Tap,
    val savedWords: Set<String> = emptySet(),
    val useChatGptTranslation: Boolean = true,
    val phraseCopyEnabled: Boolean = true,
    val wordContextExampleEnabled: Boolean = true,
    val readerFontSizeLevel: Int = ReaderFontSize.DEFAULT_LEVEL,
    val readerLineSpacingPercent: Int = ReaderLineSpacing.DEFAULT_PERCENT,
    val isImporting: Boolean = false,
    val error: String? = null,
    val addToast: String? = null,
)

class ReaderViewModel(
    private val repository: ProficonRepository,
    private val translator: WordTranslator = WordTranslator(),
) : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()
    private var savedWordsJob: Job? = null
    private var sessionStartMs: Long = 0L
    private var sessionStartParagraph: Int = 0
    private var sessionMaxParagraph: Int = 0

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        useChatGptTranslation = settings.useChatGptTranslation,
                        phraseCopyEnabled = settings.phraseCopyEnabled,
                        wordContextExampleEnabled = settings.wordContextExampleEnabled,
                        readerFontSizeLevel = settings.readerFontSizeLevel,
                        readerLineSpacingPercent = settings.readerLineSpacingPercent,
                        cursorMode = if (!settings.phraseCopyEnabled) {
                            SubtitleCursorMode.Tap
                        } else {
                            current.cursorMode
                        },
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.readerBooks.collect { books ->
                _state.update { it.copy(books = books) }
            }
        }
    }

    fun openBook(bookId: String) {
        savedWordsJob?.cancel()
        viewModelScope.launch {
            try {
                val book = repository.getReaderBook(bookId) ?: return@launch
                repository.ensureReaderCollection(book.id, book.title)
                observeSavedWords(book.id)
                val paragraphs = withContext(Dispatchers.Default) {
                    prepareReaderParagraphs(book.content)
                }
                sessionStartMs = System.currentTimeMillis()
                sessionStartParagraph = book.scrollParagraph
                sessionMaxParagraph = book.scrollParagraph
                _state.update {
                    it.copy(
                        activeBookId = book.id,
                        activeBookTitle = book.title,
                        paragraphs = paragraphs,
                        scrollParagraph = book.scrollParagraph,
                        bookmarkParagraph = book.bookmarkParagraph,
                        pendingJumpParagraph = null,
                        selectedWord = null,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Не удалось открыть книгу.") }
            }
        }
    }

    fun closeBook() {
        val bookId = _state.value.activeBookId ?: return
        val paragraph = _state.value.scrollParagraph
        val paragraphs = _state.value.paragraphs
        val from = sessionStartParagraph.coerceAtMost(sessionMaxParagraph)
        val to = sessionMaxParagraph.coerceAtMost(paragraphs.lastIndex.coerceAtLeast(0))
        val wordsRead = if (paragraphs.isNotEmpty() && to >= from) {
            paragraphs.subList(from, to + 1)
                .sumOf { para ->
                    para.split(Regex("\\s+")).count { token -> token.any { it.isLetter() } }
                }
        } else {
            0
        }
        val minutes = if (sessionStartMs > 0L) {
            ((System.currentTimeMillis() - sessionStartMs) / 60_000)
                .toInt()
                .coerceAtLeast(if (wordsRead > 0) 1 else 0)
        } else {
            0
        }
        savedWordsJob?.cancel()
        viewModelScope.launch {
            repository.updateReaderScrollParagraph(bookId, paragraph)
            if (wordsRead > 0 || minutes > 0) {
                repository.recordReaderSession(wordsRead, minutes)
            }
        }
        sessionStartMs = 0L
        sessionStartParagraph = 0
        sessionMaxParagraph = 0
        _state.update {
            it.copy(
                activeBookId = null,
                activeBookTitle = null,
                paragraphs = emptyList(),
                scrollParagraph = 0,
                bookmarkParagraph = null,
                bookmarkFeedback = null,
                pendingJumpParagraph = null,
                selectedWord = null,
                savedWords = emptySet(),
            )
        }
    }

    fun consumePendingJump() {
        _state.update { it.copy(pendingJumpParagraph = null) }
    }

    fun jumpToParagraph(paragraph: Int) {
        val last = _state.value.paragraphs.lastIndex.coerceAtLeast(0)
        val target = paragraph.coerceIn(0, last)
        _state.update {
            it.copy(
                scrollParagraph = target,
                pendingJumpParagraph = target,
            )
        }
        sessionMaxParagraph = maxOf(sessionMaxParagraph, target)
    }

    fun jumpToPage(pageIndex: Int) {
        val pageSize = ReaderPaging.PARAGRAPHS_PER_PAGE
        jumpToParagraph(pageIndex.coerceAtLeast(0) * pageSize)
    }

    fun toggleBookmark() {
        val bookId = _state.value.activeBookId ?: return
        val current = _state.value.scrollParagraph
        val existing = _state.value.bookmarkParagraph
        viewModelScope.launch {
            if (existing == current) {
                repository.clearReaderBookmark(bookId)
                _state.update {
                    it.copy(
                        bookmarkParagraph = null,
                        bookmarkFeedback = ReaderBookmarkFeedback(pageNumber = null),
                    )
                }
            } else {
                val page = pageLabelForParagraph(current)
                repository.setReaderBookmark(bookId, current)
                _state.update {
                    it.copy(
                        bookmarkParagraph = current,
                        bookmarkFeedback = ReaderBookmarkFeedback(pageNumber = page),
                    )
                }
            }
        }
    }

    fun clearBookmarkFeedback() {
        _state.update { it.copy(bookmarkFeedback = null) }
    }

    fun goToBookmark() {
        val bookmark = _state.value.bookmarkParagraph ?: return
        jumpToParagraph(bookmark)
    }

    fun updateScrollParagraph(index: Int) {
        val idx = index.coerceAtLeast(0)
        sessionMaxParagraph = maxOf(sessionMaxParagraph, idx)
        _state.update { it.copy(scrollParagraph = idx) }
    }

    fun importBook(title: String, content: String, sourceUri: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, error = null) }
            try {
                val bookId = repository.importReaderBook(title, content, sourceUri)
                _state.update { it.copy(isImporting = false) }
                openBook(bookId)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isImporting = false,
                        error = e.message ?: "Не удалось импортировать файл.",
                    )
                }
            }
        }
    }

    fun setReaderFontSizeLevel(level: Int) {
        val clamped = level.coerceIn(ReaderFontSize.MIN_LEVEL, ReaderFontSize.MAX_LEVEL)
        _state.update { it.copy(readerFontSizeLevel = clamped) }
        viewModelScope.launch {
            repository.setReaderFontSizeLevel(clamped)
        }
    }

    fun setReaderLineSpacingPercent(percent: Int) {
        val clamped = percent.coerceIn(ReaderLineSpacing.MIN_PERCENT, ReaderLineSpacing.MAX_PERCENT)
        _state.update { it.copy(readerLineSpacingPercent = clamped) }
        viewModelScope.launch {
            repository.setReaderLineSpacingPercent(clamped)
        }
    }

    fun deleteBook(bookId: String) {
        if (bookId == ReaderDemoBooks.DEMO_ID) {
            _state.update { it.copy(error = "Демо-книгу нельзя удалить.") }
            return
        }
        viewModelScope.launch {
            if (_state.value.activeBookId == bookId) {
                closeBook()
            }
            repository.deleteReaderBook(bookId)
        }
    }

    fun setCursorMode(mode: SubtitleCursorMode) {
        if (!_state.value.phraseCopyEnabled && mode == SubtitleCursorMode.Select) return
        _state.update { it.copy(cursorMode = mode, selectedWord = null) }
    }

    fun onWordClicked(pt: String, example: String?) {
        val contextExample = if (_state.value.wordContextExampleEnabled) example else null
        translateAndShow(pt, contextExample, isPhrase = false)
    }

    fun onPhraseSelected(raw: String, example: String?) {
        val phrase = PhraseSelectionLimits.clampToMaxWords(
            raw.trim().replace(Regex("\\s+"), " "),
        )
        if (phrase.isEmpty()) return
        translateAndShow(phrase, example, isPhrase = true)
    }

    fun clearSelectedWord() {
        _state.update { it.copy(selectedWord = null) }
    }

    fun clearAddToast() {
        _state.update { it.copy(addToast = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun showError(message: String) {
        _state.update { it.copy(error = message) }
    }

    fun addSelectedWordToDictionary() {
        val word = _state.value.selectedWord ?: return
        if (word.isPhrase && !_state.value.phraseCopyEnabled) return
        val ru = word.ru?.trim().orEmpty()
        if (ru.isEmpty() || word.isAdded) return

        val bookId = _state.value.activeBookId ?: return

        viewModelScope.launch {
            try {
                repository.addWordFromReader(
                    bookId = bookId,
                    bookTitle = _state.value.activeBookTitle.orEmpty(),
                    pt = word.pt,
                    ru = ru,
                    example = word.example,
                )
                val normalized = WordNormalizer.normalize(word.pt)
                _state.update { current ->
                    current.copy(
                        selectedWord = word.copy(isAdded = true),
                        savedWords = current.savedWords + normalized,
                        addToast = "«${word.pt}» добавлено в словарь",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Не удалось добавить в словарь.") }
            }
        }
    }

    private fun observeSavedWords(bookId: String) {
        savedWordsJob = viewModelScope.launch {
            repository.observeSavedWordsForBook(bookId).collect { words ->
                _state.update { it.copy(savedWords = words) }
            }
        }
    }

    private fun translateAndShow(text: String, example: String?, isPhrase: Boolean) {
        val clean = text.trim()
        if (clean.isEmpty() || !clean.any { it.isLetter() }) return

        val alreadySaved = _state.value.savedWords.contains(WordNormalizer.normalize(clean))

        viewModelScope.launch {
            _state.update {
                it.copy(
                    selectedWord = SelectedWord(
                        pt = clean,
                        example = example,
                        isTranslating = true,
                        isAdded = alreadySaved,
                        isPhrase = isPhrase,
                    ),
                    error = null,
                )
            }
            try {
                val translation = translator.translatePtRu(clean, _state.value.useChatGptTranslation)
                _state.update {
                    it.copy(
                        selectedWord = SelectedWord(
                            pt = clean,
                            ru = translation.ru,
                            example = example,
                            ptInfinitive = if (!isPhrase) translation.ptInfinitive else null,
                            isTranslating = false,
                            isAdded = alreadySaved,
                            isPhrase = isPhrase,
                        ),
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        selectedWord = SelectedWord(
                            pt = clean,
                            ru = null,
                            example = example,
                            ptInfinitive = null,
                            isTranslating = false,
                            isAdded = alreadySaved,
                            isPhrase = isPhrase,
                        ),
                        error = e.message ?: "Не удалось перевести.",
                    )
                }
            }
        }
    }

    private fun prepareReaderParagraphs(content: String): List<String> {
        val normalizedContent = content.replace("\r\n", "\n")
        val base = splitBookParagraphs(content)
        val source = if (base.size <= 1) {
            normalizedContent
                .split(Regex("\n+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } else {
            base
        }

        val chunked = source.flatMap { splitLargeParagraph(it) }
        if (chunked.isNotEmpty()) return chunked

        val singleLineFallback = normalizedContent
            .replace(Regex("\\s+"), " ")
            .trim()
            .let { flat ->
                if (flat.length > 800) {
                    flat.chunked(500).map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }
            }
        if (singleLineFallback.isNotEmpty()) return singleLineFallback

        return splitLargeParagraph(normalizedContent.replace(Regex("\\s+"), " ").trim())
    }

    private fun splitLargeParagraph(text: String): List<String> {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= 600) return listOf(clean)

        val sentences = clean.split(Regex("(?<=[.!?;:])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (sentences.size <= 1) {
            return clean.chunked(500)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        val result = mutableListOf<String>()
        val builder = StringBuilder()
        sentences.forEach { sentence ->
            val plus = if (builder.isEmpty()) sentence else " $sentence"
            if (builder.length + plus.length > 650 && builder.isNotEmpty()) {
                result += builder.toString().trim()
                builder.clear()
                builder.append(sentence)
            } else {
                builder.append(plus)
            }
        }
        if (builder.isNotEmpty()) {
            result += builder.toString().trim()
        }

        return result.flatMap { chunk ->
            if (chunk.length <= 700) listOf(chunk)
            else chunk.chunked(max(350, minOf(550, chunk.length / 2)))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    private fun pageLabelForParagraph(paragraph: Int): Int =
        (paragraph / ReaderPaging.PARAGRAPHS_PER_PAGE) + 1
}

class ReaderViewModelFactory(
    private val repository: ProficonRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReaderViewModel::class.java)) {
            return ReaderViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
