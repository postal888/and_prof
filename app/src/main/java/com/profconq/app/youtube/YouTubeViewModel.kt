package com.profconq.app.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.profconq.app.data.model.SubtitleCursorMode
import com.profconq.app.data.model.SubtitleFontSize
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.data.repository.WordNormalizer
import com.profconq.app.ui.components.translation.PhraseSelectionLimits
import com.profconq.app.ui.i18n.AppLanguage
import com.profconq.app.ui.i18n.SubtitleLanguage
import com.profconq.app.ui.i18n.UiStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedWord(
    val pt: String,
    val ru: String? = null,
    val example: String? = null,
    val ptInfinitive: String? = null,
    val isTranslating: Boolean = false,
    val isAdded: Boolean = false,
    val isPhrase: Boolean = false,
)

data class YouTubeUiState(
    val inputMode: YouTubeInputMode = YouTubeInputMode.Search,
    val videoSearchQuery: String = "",
    val videoSearchFilter: VideoSearchFilter = VideoSearchFilter.Full,
    val videoResults: List<YouTubeVideoResult> = emptyList(),
    val isSearchingVideos: Boolean = false,
    val selectedVideoTitle: String? = null,
    val urlInput: String = "",
    val videoId: String? = null,
    val subtitleLines: List<SubtitleLine> = emptyList(),
    val subtitleQuery: String = "",
    val matchIndices: List<Int> = emptyList(),
    val activeMatchIndex: Int = 0,
    val activeLineId: String? = null,
    val playbackSec: Float = 0f,
    val isLoading: Boolean = false,
    val isReloadingSubtitles: Boolean = false,
    val error: String? = null,
    val languageLabel: String? = null,
    val isSearchExpanded: Boolean = true,
    val selectedWord: SelectedWord? = null,
    val cursorMode: SubtitleCursorMode = SubtitleCursorMode.Tap,
    val savedWords: Set<String> = emptySet(),
    val addToast: String? = null,
    val useChatGptTranslation: Boolean = true,
    val phraseCopyEnabled: Boolean = true,
    val wordContextExampleEnabled: Boolean = true,
    val subtitleFontSizeLevel: Int = SubtitleFontSize.DEFAULT_LEVEL,
    val watchHistory: List<YouTubeWatchHistoryItem> = emptyList(),
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val durationSec: Float = 0f,
    val durationLabel: String? = null,
    val videoThumbnailUrl: String? = null,
    val videoChannel: String = "",
)

class YouTubeViewModel(
    private val repository: ProfconqRepository,
    private val fetcher: YouTubeTranscriptFetcher = YouTubeTranscriptFetcher(),
    private val searchService: YouTubeSearchService = YouTubeSearchService(),
    private val translator: WordTranslator = WordTranslator(),
) : ViewModel() {
    private val _state = MutableStateFlow(YouTubeUiState())
    val state: StateFlow<YouTubeUiState> = _state.asStateFlow()
    private var savedWordsJob: Job? = null
    private var lastSearchResults: List<YouTubeVideoResult> = emptyList()
    private var lastPlaybackSec: Float = 0f
    private var pendingYoutubeSeconds: Int = 0
    private var lastPositionPersistMs: Long = 0L
    private var lastPersistedPositionSec: Float = 0f
    private var activeVideoId: String? = null
    private var loadVideoJob: Job? = null
    private var reloadSubtitlesJob: Job? = null
    private var subtitleLangCode: String = "pt"
    private var translationSourceLanguage: Int = SubtitleLanguage.PT
    private var translationTargetLanguage: Int = SubtitleLanguage.RU
    private var uiLanguageCode: Int = AppLanguage.DEFAULT.storageCode

    private fun strings(): UiStrings = UiStrings.forLanguage(AppLanguage.fromStorage(uiLanguageCode))

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                uiLanguageCode = settings.uiLanguage
                translationSourceLanguage = settings.translationSourceLanguage
                translationTargetLanguage = settings.translationTargetLanguage
                subtitleLangCode = SubtitleLanguage.toCode(translationSourceLanguage)
                _state.update { current ->
                    current.copy(
                        useChatGptTranslation = settings.useChatGptTranslation,
                        phraseCopyEnabled = settings.phraseCopyEnabled,
                        wordContextExampleEnabled = settings.wordContextExampleEnabled,
                        subtitleFontSizeLevel = settings.subtitleFontSizeLevel,
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
            repository.youtubeWatchHistory.collect { history ->
                _state.update { it.copy(watchHistory = history) }
            }
        }

        viewModelScope.launch {
            state.collect { YouTubeNowPlayingHub.publish(it) }
        }
    }

    val isWatchMode: Boolean
        get() = _state.value.videoId != null

    fun setInputMode(mode: YouTubeInputMode) {
        _state.update { it.copy(inputMode = mode, error = null) }
    }

    fun setVideoSearchQuery(value: String) {
        _state.update { it.copy(videoSearchQuery = value, error = null) }
    }

    fun setVideoSearchFilter(filter: VideoSearchFilter) {
        _state.update { current ->
            val filtered = applyVideoSearchFilter(lastSearchResults, filter)
            current.copy(
                videoSearchFilter = filter,
                videoResults = filtered,
                error = if (filtered.isEmpty() && lastSearchResults.isNotEmpty()) {
                    filterEmptyMessage(filter)
                } else {
                    null
                },
            )
        }
    }

    fun setUrlInput(value: String) {
        _state.update { it.copy(urlInput = value, error = null) }
    }

    fun setSubtitleQuery(value: String) {
        _state.update { current ->
            val matches = findMatches(current.subtitleLines, value)
            current.copy(
                subtitleQuery = value,
                matchIndices = matches,
                activeMatchIndex = 0,
            )
        }
    }

    fun toggleSearchExpanded() {
        _state.update { it.copy(isSearchExpanded = !it.isSearchExpanded) }
    }

    fun setCursorMode(mode: SubtitleCursorMode) {
        if (!_state.value.phraseCopyEnabled && mode == SubtitleCursorMode.Select) return
        _state.update { it.copy(cursorMode = mode, selectedWord = null) }
    }

    fun clearAddToast() {
        _state.update { it.copy(addToast = null) }
    }

    fun searchVideos() {
        val query = _state.value.videoSearchQuery.trim()
        if (query.isEmpty()) {
            _state.update { it.copy(error = strings().ytEmptySearchQuery) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSearchingVideos = true, error = null, videoResults = emptyList()) }
            try {
                val filter = _state.value.videoSearchFilter
                lastSearchResults = searchService.searchVideos(query)
                val filtered = applyVideoSearchFilter(lastSearchResults, filter)
                if (filtered.isEmpty()) {
                    throw IllegalStateException(filterEmptyMessage(filter))
                }
                _state.update {
                    it.copy(
                        isSearchingVideos = false,
                        videoResults = filtered,
                    )
                }
            } catch (e: Exception) {
                lastSearchResults = emptyList()
                _state.update {
                    it.copy(
                        isSearchingVideos = false,
                        error = e.message ?: strings().ytSearchFailed,
                    )
                }
            }
        }
    }

    private fun applyVideoSearchFilter(
        results: List<YouTubeVideoResult>,
        filter: VideoSearchFilter,
    ): List<YouTubeVideoResult> {
        val filtered = when (filter) {
            VideoSearchFilter.All -> results
            VideoSearchFilter.Full -> results.filter { !it.isShort }
            VideoSearchFilter.Shorts -> results.filter { it.isShort }
        }
        return filtered.take(20)
    }

    private fun filterEmptyMessage(filter: VideoSearchFilter): String = when (filter) {
        VideoSearchFilter.All -> strings().ytNoResultsAll
        VideoSearchFilter.Full -> strings().ytNoResultsFull
        VideoSearchFilter.Shorts -> strings().ytNoResultsShorts
    }

    fun selectSearchResult(result: YouTubeVideoResult) {
        _state.update {
            it.copy(
                urlInput = "https://www.youtube.com/watch?v=${result.videoId}",
                selectedVideoTitle = result.title,
                videoResults = emptyList(),
                error = null,
            )
        }
        loadVideoById(
            videoId = result.videoId,
            title = result.title,
            channel = result.channel,
            thumbnailUrl = result.thumbnailUrl,
            duration = result.duration,
            isShort = result.isShort,
            publishedAtMillis = result.publishedAtMillis,
        )
    }

    fun loadVideo(rawInput: String? = null) {
        val input = rawInput ?: _state.value.urlInput
        val videoId = YouTubeTranscriptFetcher.extractVideoId(input)
        if (videoId == null) {
            _state.update { it.copy(error = strings().ytInvalidUrl) }
            return
        }
        loadVideoById(
            videoId = videoId,
            title = _state.value.selectedVideoTitle,
        )
    }

    fun openFromHistory(item: YouTubeWatchHistoryItem) {
        loadVideoById(
            videoId = item.videoId,
            title = item.title,
            channel = item.channel,
            thumbnailUrl = item.thumbnailUrl,
            duration = item.duration,
            isShort = item.isShort,
            resumeSec = item.lastPositionSec,
            publishedAtMillis = item.publishedAtMillis,
        )
    }

    fun clearWatchHistory() {
        viewModelScope.launch { repository.clearYouTubeWatchHistory() }
    }

    fun removeFromHistory(videoId: String) {
        viewModelScope.launch { repository.deleteYouTubeWatchHistoryItem(videoId) }
    }

    fun reloadSubtitles() {
        val videoId = _state.value.videoId ?: return

        reloadSubtitlesJob?.cancel()
        reloadSubtitlesJob = viewModelScope.launch {
            _state.update { it.copy(isReloadingSubtitles = true, error = null) }
            try {
                val result = fetcher.fetchTranscript(videoId, subtitleLangCode)
                if (!isActive) return@launch
                val matches = findMatches(result.lines, _state.value.subtitleQuery)
                _state.update {
                    it.copy(
                        isReloadingSubtitles = false,
                        subtitleLines = result.lines,
                        languageLabel = result.language,
                        matchIndices = matches,
                        activeMatchIndex = 0,
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isActive) return@launch
                _state.update {
                    it.copy(
                        isReloadingSubtitles = false,
                        error = e.message ?: strings().ytReloadSubtitlesFailed,
                    )
                }
            }
        }
    }

    private fun loadVideoById(
        videoId: String,
        title: String? = null,
        channel: String = "",
        thumbnailUrl: String? = null,
        duration: String? = null,
        isShort: Boolean = false,
        resumeSec: Float? = null,
        publishedAtMillis: Long? = null,
    ) {
        flushYoutubeSeconds()
        persistWatchPosition()
        loadVideoJob?.cancel()

        lastPlaybackSec = resumeSec ?: 0f
        lastPersistedPositionSec = resumeSec ?: 0f
        lastPositionPersistMs = System.currentTimeMillis()
        activeVideoId = videoId
        observeSavedWords(videoId)

        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                videoId = videoId,
                urlInput = "https://www.youtube.com/watch?v=$videoId",
                selectedVideoTitle = title,
                subtitleLines = emptyList(),
                matchIndices = emptyList(),
                activeMatchIndex = 0,
                activeLineId = null,
                playbackSec = resumeSec ?: 0f,
                selectedWord = null,
                isSearchExpanded = false,
                videoResults = emptyList(),
                isPlaying = false,
                isPaused = false,
                durationSec = 0f,
                durationLabel = duration,
                videoThumbnailUrl = thumbnailUrl,
                videoChannel = channel,
            )
        }

        loadVideoJob = viewModelScope.launch {
            try {
                repository.ensureYouTubeCollection(videoId, title.orEmpty())
                repository.recordYouTubeWatch(
                    videoId = videoId,
                    title = title.orEmpty(),
                    channel = channel,
                    thumbnailUrl = thumbnailUrl,
                    duration = duration,
                    isShort = isShort,
                    positionSec = resumeSec ?: 0f,
                    publishedAtMillis = publishedAtMillis,
                )
                val result = fetcher.fetchTranscript(videoId, subtitleLangCode)
                if (!isActive) return@launch
                val matches = findMatches(result.lines, _state.value.subtitleQuery)
                _state.update {
                    it.copy(
                        isLoading = false,
                        subtitleLines = result.lines,
                        languageLabel = result.language,
                        matchIndices = matches,
                        activeMatchIndex = 0,
                        error = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isActive) return@launch
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: strings().ytLoadSubtitlesFailed,
                    )
                }
            }
        }
    }

    private fun observeSavedWords(videoId: String) {
        savedWordsJob?.cancel()
        savedWordsJob = viewModelScope.launch {
            repository.observeSavedWordsForVideo(videoId).collect { words ->
                _state.update { it.copy(savedWords = words) }
            }
        }
    }

    fun updatePlaybackSec(sec: Float) {
        val videoId = _state.value.videoId
        if (videoId != null) {
            val delta = sec - lastPlaybackSec
            if (delta in 0.1f..5f) {
                pendingYoutubeSeconds += delta.toInt().coerceAtLeast(1)
                if (pendingYoutubeSeconds >= 30) {
                    flushYoutubeSeconds()
                }
            }
            lastPlaybackSec = sec

            val now = System.currentTimeMillis()
            if (now - lastPositionPersistMs >= 15_000 && kotlin.math.abs(sec - lastPersistedPositionSec) >= 5f) {
                lastPositionPersistMs = now
                lastPersistedPositionSec = sec
                viewModelScope.launch { repository.updateYouTubeWatchPosition(videoId, sec) }
            }
        }

        _state.update { current ->
            val active = current.subtitleLines.firstOrNull { line ->
                sec >= line.startSec && sec < line.endSec
            }
            current.copy(
                playbackSec = sec,
                activeLineId = active?.id,
            )
        }
    }

    private fun flushYoutubeSeconds() {
        val seconds = pendingYoutubeSeconds
        pendingYoutubeSeconds = 0
        if (seconds <= 0) return
        viewModelScope.launch { repository.recordYouTubeSeconds(seconds) }
    }

    private fun persistWatchPosition() {
        val videoId = activeVideoId ?: return
        val position = lastPlaybackSec
        if (position <= 0f) return
        viewModelScope.launch { repository.updateYouTubeWatchPosition(videoId, position) }
    }

    override fun onCleared() {
        flushYoutubeSeconds()
        persistWatchPosition()
        YouTubeNowPlayingHub.clear()
        super.onCleared()
    }

    fun onPlayerState(playing: Boolean, paused: Boolean) {
        _state.update { it.copy(isPlaying = playing, isPaused = paused) }
    }

    fun onVideoDuration(sec: Float) {
        if (sec <= 0f) return
        _state.update { it.copy(durationSec = sec) }
    }

    fun onPlayerDetached() {
        _state.update { it.copy(isPlaying = false, isPaused = false) }
    }

    fun seekTargetSec(): Float? {
        val state = _state.value
        if (state.subtitleQuery.isNotBlank() && state.matchIndices.isNotEmpty()) {
            val lineIndex = state.matchIndices[state.activeMatchIndex.coerceIn(state.matchIndices.indices)]
            return state.subtitleLines.getOrNull(lineIndex)?.startSec
        }
        return null
    }

    fun onLineClicked(line: SubtitleLine) {
        _state.update { it.copy(activeLineId = line.id) }
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
                val translation = translator.translate(
                    text = clean,
                    fromLang = translationSourceLanguage,
                    toLang = translationTargetLanguage,
                    useChatGpt = _state.value.useChatGptTranslation,
                )
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
                        error = strings().userVisibleError(e, strings().ytTranslateFailed),
                    )
                }
            }
        }
    }

    fun clearSelectedWord() {
        _state.update { it.copy(selectedWord = null) }
    }

    fun addSelectedWordToDictionary() {
        val word = _state.value.selectedWord ?: return
        if (word.isPhrase && !_state.value.phraseCopyEnabled) return
        val ru = word.ru?.trim().orEmpty()
        if (ru.isEmpty() || word.isAdded) return

        val videoId = _state.value.videoId ?: return

        viewModelScope.launch {
            try {
                repository.addWordFromYouTube(
                    videoId = videoId,
                    videoTitle = _state.value.selectedVideoTitle.orEmpty(),
                    pt = word.pt,
                    ru = ru,
                    example = word.example,
                )
                val normalized = WordNormalizer.normalize(word.pt)
                _state.update { current ->
                    current.copy(
                        selectedWord = word.copy(isAdded = true),
                        savedWords = current.savedWords + normalized,
                        addToast = strings().wordAddedToDictionary(word.pt),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: strings().ytAddToDictionaryFailed) }
            }
        }
    }

    fun nextMatch() {
        _state.update { current ->
            if (current.matchIndices.isEmpty()) return@update current
            val next = (current.activeMatchIndex + 1) % current.matchIndices.size
            val line = current.subtitleLines[current.matchIndices[next]]
            current.copy(activeMatchIndex = next, activeLineId = line.id)
        }
    }

    fun previousMatch() {
        _state.update { current ->
            if (current.matchIndices.isEmpty()) return@update current
            val prev = if (current.activeMatchIndex == 0) {
                current.matchIndices.lastIndex
            } else {
                current.activeMatchIndex - 1
            }
            val line = current.subtitleLines[current.matchIndices[prev]]
            current.copy(activeMatchIndex = prev, activeLineId = line.id)
        }
    }

    fun visibleLines(): List<SubtitleLine> {
        val state = _state.value
        if (state.subtitleQuery.isBlank()) return state.subtitleLines
        return state.matchIndices.mapNotNull { index -> state.subtitleLines.getOrNull(index) }
    }

    fun isWordSaved(pt: String): Boolean {
        return _state.value.savedWords.contains(WordNormalizer.normalize(pt))
    }

    private fun findMatches(lines: List<SubtitleLine>, query: String): List<Int> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return lines.mapIndexedNotNull { index, line ->
            if (line.text.contains(q, ignoreCase = true)) index else null
        }
    }
}

class YouTubeViewModelFactory(
    private val repository: ProfconqRepository,
    private val authTokenProvider: suspend (Boolean) -> String? = { null },
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YouTubeViewModel::class.java)) {
            return YouTubeViewModel(
                repository = repository,
                translator = WordTranslator(authTokenProvider = authTokenProvider),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
