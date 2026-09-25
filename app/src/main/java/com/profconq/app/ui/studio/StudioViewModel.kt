package com.profconq.app.ui.studio

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.profconq.app.data.model.Collection
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import com.profconq.app.studio.StudioCollection
import com.profconq.app.studio.StudioDemoSeeder
import com.profconq.app.studio.StudioNowPlayingHub
import com.profconq.app.studio.StudioPlaybackCommands
import com.profconq.app.studio.StudioPrefs
import com.profconq.app.studio.StudioRenderMeta
import com.profconq.app.studio.StudioStore
import com.profconq.app.studio.StudioSyncService
import com.profconq.app.studio.StudioTtsPlayer
import com.profconq.app.studio.StudioVoice
import com.profconq.app.studio.buildStudioStages
import com.profconq.app.studio.pauseAfterStage
import com.profconq.app.studio.speedForKind
import com.profconq.app.studio.withStagePause
import com.profconq.app.studio.withStageSpeed
import com.profconq.app.ProfconqApplication
import com.profconq.app.studio.studioCollectionAudioHash
import com.profconq.app.studio.studioCollectionAudioJobs
import com.profconq.app.studio.studioCollectionIsRendered
import com.profconq.app.studio.studioDirKey
import com.profconq.app.ui.i18n.SubtitleLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StudioUiState(
    val collections: List<StudioCollection> = emptyList(),
    val activeId: String? = null,
    val activeCards: List<WordCard> = emptyList(),
    val dictionaryFolders: List<Collection> = emptyList(),
    val pickerOpen: Boolean = false,
    val prefs: StudioPrefs = StudioPrefs(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val playing: Boolean = false,
    val paused: Boolean = false,
    val queue: List<WordCard> = emptyList(),
    val index: Int = 0,
    val reveal: Int = -1,
    val status: String = "",
    val collectionTitle: String = "",
    val editorOpen: Boolean = false,
    val setupCollapsed: Boolean = true,
    val isRendering: Boolean = false,
    val renderDone: Int = 0,
    val renderTotal: Int = 0,
    val renderMessage: String? = null,
    val cardElapsedMs: Long = 0L,
    val cardDurationMs: Long = 0L,
) {
    val sessionOpen: Boolean get() = queue.isNotEmpty()
    val current: WordCard? get() = queue.getOrNull(index)
}

class StudioViewModel(
    private val repository: ProfconqRepository,
    private val store: StudioStore,
    private val syncService: StudioSyncService,
    private val tts: StudioTtsPlayer,
    private val intensity: com.profconq.app.study.StudyIntensityStore? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    private fun strings() = ProfconqApplication.uiStrings()

    private var playJob: Job? = null
    private var renderJob: Job? = null
    private var tickJob: Job? = null
    private var generation = 0
    private var renderGeneration = 0
    private var cardClockStart = 0L
    private var cardPausedAccum = 0L
    private var cardFreezeAt = 0L
    private var clockRunning = false
    private var clockHeld = false
    private var cardStartMs: LongArray = longArrayOf()

    private val playbackCommands = object : StudioPlaybackCommands {
        override fun playPause() = togglePlayPause()
        override fun next() = nextCard()
        override fun prev() = prevCard()
        override fun stop() = stopSession()
    }

    private var studySourceLang: Int = SubtitleLanguage.PT
    private var studyTargetLang: Int = SubtitleLanguage.RU

    private fun StudioPrefs.withStudyLangs(): StudioPrefs =
        copy(sourceLang = studySourceLang, targetLang = studyTargetLang)

    init {
        StudioNowPlayingHub.bind(playbackCommands)
        viewModelScope.launch {
            repository.collections.collect { folders ->
                _state.update { it.copy(dictionaryFolders = folders.filter { c -> c.cards.isNotEmpty() }) }
            }
        }
        viewModelScope.launch {
            state.collect { StudioNowPlayingHub.publish(it) }
        }
        viewModelScope.launch {
            repository.settings.collect { settings ->
                studySourceLang = settings.translationSourceLanguage
                studyTargetLang = settings.translationTargetLanguage
                val current = _state.value.prefs
                if (current.sourceLang == studySourceLang && current.targetLang == studyTargetLang) {
                    return@collect
                }
                _state.update { it.copy(prefs = current.withStudyLangs()) }
            }
        }
        reloadLocal()
    }

    fun seedDemoIfNeeded(context: android.content.Context) {
        viewModelScope.launch {
            runCatching { StudioDemoSeeder.ensureSeeded(context, repository, store) }
            reloadLocal()
        }
    }

    fun reloadLocal() {
        val cols = store.loadCollections()
        val active = store.activeCollectionId() ?: cols.firstOrNull()?.id
        _state.update {
            it.copy(
                collections = cols,
                activeId = active,
                prefs = store.loadPrefs().withStudyLangs(),
                collectionTitle = cols.find { c -> c.id == active }?.title.orEmpty(),
                setupCollapsed = store.isSetupCollapsed(),
            )
        }
        refreshActiveCards()
    }

    fun selectCollection(id: String) {
        stopInternal(keepQueue = false)
        store.setActiveCollectionId(id)
        val col = store.loadCollections().find { it.id == id }
        _state.update {
            it.copy(
                activeId = id,
                collectionTitle = col?.title.orEmpty(),
                queue = emptyList(),
                playing = false,
                paused = false,
                reveal = -1,
                status = "",
                cardElapsedMs = 0L,
                cardDurationMs = 0L,
            )
        }
        refreshActiveCards()
    }

    fun editCollection(id: String) {
        if (id != _state.value.activeId) {
            selectCollection(id)
        } else {
            stopInternal(keepQueue = false)
            _state.update {
                it.copy(
                    queue = emptyList(),
                    playing = false,
                    paused = false,
                    reveal = -1,
                    status = "",
                    cardElapsedMs = 0L,
                    cardDurationMs = 0L,
                )
            }
        }
        store.setSetupCollapsed(false)
        _state.update { it.copy(setupCollapsed = false) }
    }

    fun updatePrefs(prefs: StudioPrefs) {
        val next = prefs.withStudyLangs()
        store.savePrefs(next)
        _state.update { it.copy(prefs = next) }
    }

    fun setDir(ptToRu: Boolean) = updatePrefs(_state.value.prefs.copy(dirPtToRu = ptToRu))
    fun toggleDir() = setDir(!_state.value.prefs.dirPtToRu)
    fun setVoice(voice: String) = updatePrefs(_state.value.prefs.copy(voice = StudioVoice.fromKey(voice).key))
    fun setSpeed(speed: Float) = updatePrefs(_state.value.prefs.copy(speed = speed, speedWord = speed))
    fun setStageSpeed(kind: String, value: Float) = updatePrefs(_state.value.prefs.withStageSpeed(kind, value))
    fun setStagePause(kind: String, value: Float) = updatePrefs(_state.value.prefs.withStagePause(kind, value))
    fun setPauseAfter(v: Float) = setStagePause("word", v)
    fun setPauseBetween(v: Float) = setStagePause("tr", v)
    fun setPauseCards(v: Float) = setStagePause("cards", v)
    fun toggleSpeakWord() = updatePrefs(_state.value.prefs.copy(speakWord = !_state.value.prefs.speakWord))
    fun toggleSpeakTr() = updatePrefs(_state.value.prefs.copy(speakTr = !_state.value.prefs.speakTr))
    fun toggleSpeakEx() = updatePrefs(_state.value.prefs.copy(speakEx = !_state.value.prefs.speakEx))
    fun toggleSpeakExRu() = updatePrefs(_state.value.prefs.copy(speakExRu = !_state.value.prefs.speakExRu))
    fun toggleSpeakNum() = updatePrefs(_state.value.prefs.copy(speakNum = !_state.value.prefs.speakNum))
    fun toggleLoop() = updatePrefs(_state.value.prefs.copy(loop = !_state.value.prefs.loop))
    fun toggleShuffle() = updatePrefs(_state.value.prefs.copy(shuffle = !_state.value.prefs.shuffle))

    fun createCollection(title: String) {
        val col = syncService.createLocalCollection(title)
        reloadLocal()
        editCollection(col.id)
        openPicker()
        persistCloud()
    }

    fun renameCollection(id: String, title: String) {
        syncService.renameCollection(id, title)
        reloadLocal()
        persistCloud()
    }

    fun deleteCollection(id: String) {
        stopInternal(keepQueue = false)
        syncService.deleteCollection(id)
        reloadLocal()
        persistCloud()
    }

    fun duplicateCollection(id: String) {
        val copy = syncService.duplicateCollection(id) ?: return
        reloadLocal()
        selectCollection(copy.id)
        persistCloud()
    }

    fun removeWord(wordId: String) {
        val col = activeCollection() ?: return
        stopInternal(keepQueue = false)
        val next = col.wordIds.filter { it != wordId }
        syncService.updateCollectionWords(col.id, next)
        reloadLocal()
        persistCloud()
    }

    fun addWords(wordIds: List<String>) {
        val col = activeCollection() ?: run {
            syncService.createLocalCollection("Studio", wordIds)
            reloadLocal()
            persistCloud()
            return
        }
        stopInternal(keepQueue = false)
        syncService.updateCollectionWords(col.id, col.wordIds + wordIds)
        reloadLocal()
        persistCloud()
    }

    fun openPicker() = _state.update { it.copy(pickerOpen = true) }
    fun closePicker() = _state.update { it.copy(pickerOpen = false) }

    fun syncFromCloud() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncMessage = null) }
            runCatching { syncService.pullFromCloud() }
                .onSuccess { cols ->
                    reloadLocal()
                    _state.update {
                        it.copy(
                            isSyncing = false,
                            syncMessage = strings().studioSyncPulled(cols.size),
                            collections = cols,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isSyncing = false, syncMessage = strings().syncErrorGeneric) }
                }
        }
    }

    fun syncToCloud() {
        persistCloud()
    }

    fun isPlayReady(): Boolean {
        val s = _state.value
        val col = s.collections.find { it.id == s.activeId } ?: s.collections.firstOrNull()
        if (col == null || s.activeCards.isEmpty()) return false
        if (!studioCollectionIsRendered(col, s.activeCards, s.prefs)) return false
        val hash = col.render?.hash.orEmpty()
        return tts.cardMp3sReady(col.id, hash, s.activeCards.size) ||
            tts.clipsReady(s.activeCards, s.prefs)
    }

    fun isRenderStale(): Boolean {
        val s = _state.value
        val col = s.collections.find { it.id == s.activeId } ?: s.collections.firstOrNull()
        return col?.render?.hash?.isNotBlank() == true && !isPlayReady()
    }

    fun renderCollection() {
        if (_state.value.isRendering) {
            cancelRender()
            return
        }
        viewModelScope.launch {
            val col = activeCollection() ?: return@launch
            val cards = _state.value.activeCards
            if (cards.isEmpty()) {
                _state.update { it.copy(renderMessage = "empty") }
                return@launch
            }
            val prefs = _state.value.prefs
            val jobs = studioCollectionAudioJobs(cards, prefs)
            if (jobs.isEmpty()) {
                _state.update { it.copy(renderMessage = "empty") }
                return@launch
            }
            val hashReady = studioCollectionIsRendered(col, cards, prefs)
            val forceRebuild = col.render != null && !hashReady
            val hash = studioCollectionAudioHash(cards, prefs)
            renderGeneration++
            val gen = renderGeneration
            stopInternal(keepQueue = false)
            _state.update {
                it.copy(
                    isRendering = true,
                    renderDone = 0,
                    renderTotal = cards.size,
                    renderMessage = null,
                    queue = emptyList(),
                    playing = false,
                    paused = false,
                    status = "",
                )
            }
            renderJob = viewModelScope.launch {
                runCatching {
                    cards.forEachIndexed { index, card ->
                        if (gen != renderGeneration) return@launch
                        val stages = buildStudioStages(card, index + 1, prefs)
                        val clipFiles = mutableListOf<java.io.File>()
                        val pauses = mutableListOf<Float>()
                        for (stage in stages) {
                            if (gen != renderGeneration) return@launch
                            val file = tts.ensureClip(stage.text, stage.lang, prefs.voice, forceRebuild)
                                ?: error("tts")
                            clipFiles += file
                            pauses += pauseAfterStage(stage, prefs)
                        }
                        if (clipFiles.isNotEmpty()) {
                            if (pauses.isNotEmpty()) pauses[pauses.lastIndex] = 0f
                            val assembled = tts.assembleCardMp3(
                                clipFiles,
                                tts.cardMp3File(col.id, hash, index),
                                pauses,
                            )
                            if (!assembled) error("assemble")
                        }
                        _state.update { it.copy(renderDone = index + 1) }
                    }
                    if (gen != renderGeneration) return@launch
                    val meta = StudioRenderMeta(
                        hash = hash,
                        voice = prefs.voice,
                        dir = studioDirKey(prefs),
                        speakNum = prefs.speakNum,
                        speakWord = prefs.speakWord,
                        speakTr = prefs.speakTr,
                        speakEx = prefs.speakEx,
                        speakExRu = prefs.speakExRu,
                        clips = jobs.size,
                        at = System.currentTimeMillis(),
                    )
                    syncService.setCollectionRender(col.id, meta)
                    reloadLocal()
                    _state.update {
                        it.copy(
                            isRendering = false,
                            renderDone = cards.size,
                            renderTotal = cards.size,
                            renderMessage = "ok",
                            activeCards = cards,
                            activeId = col.id,
                        )
                    }
                    persistCloud()
                    if (gen == renderGeneration) {
                        startSession(col.id)
                    }
                }.onFailure {
                    if (gen != renderGeneration) return@launch
                    _state.update {
                        it.copy(isRendering = false, renderMessage = "fail")
                    }
                }
            }
        }
    }

    fun cancelRender() {
        renderGeneration++
        renderJob?.cancel()
        renderJob = null
        _state.update { it.copy(isRendering = false, renderMessage = "cancel") }
    }

    fun startSession(collectionId: String? = _state.value.activeId) {
        viewModelScope.launch {
            val col = _state.value.collections.find { it.id == collectionId }
                ?: _state.value.collections.firstOrNull()
                ?: return@launch
            val sourceCards = syncService.resolveCards(col)
            if (sourceCards.isEmpty()) {
                _state.update { it.copy(status = "empty") }
                return@launch
            }
            val prefs = _state.value.prefs
            val hash = col.render?.hash.orEmpty()
            val ready = studioCollectionIsRendered(col, sourceCards, prefs) &&
                (tts.cardMp3sReady(col.id, hash, sourceCards.size) || tts.clipsReady(sourceCards, prefs))
            if (!ready) {
                _state.update { it.copy(renderMessage = "need", activeCards = sourceCards, activeId = col.id) }
                return@launch
            }
            val queue = if (prefs.shuffle) sourceCards.shuffled() else sourceCards
            stopInternal(keepQueue = false)
            generation++
            val gen = generation
            _state.update {
                it.copy(
                    queue = queue,
                    activeCards = sourceCards,
                    index = 0,
                    reveal = -1,
                    playing = true,
                    paused = false,
                    collectionTitle = col.title,
                    status = "",
                    activeId = col.id,
                    setupCollapsed = true,
                    renderMessage = null,
                )
            }
            store.setActiveCollectionId(col.id)
            store.setSetupCollapsed(true)
            bindSessionTiming(queue, startIndex = 0, running = true)
            playJob = viewModelScope.launch { runLoop(gen) }
        }
    }

    fun closeSetup() {
        store.setSetupCollapsed(true)
        _state.update { it.copy(setupCollapsed = true, pickerOpen = false) }
    }

    fun toggleSetupCollapsed() {
        if (_state.value.setupCollapsed) {
            val id = _state.value.activeId ?: return
            editCollection(id)
        } else {
            closeSetup()
        }
    }

    fun togglePlayPause() {
        val s = _state.value
        if (s.queue.isEmpty()) {
            startSession()
            return
        }
        if (!s.playing) {
            generation++
            val gen = generation
            _state.update { it.copy(playing = true, paused = false) }
            playJob = viewModelScope.launch {
                if (cardStartMs.isEmpty() || _state.value.cardDurationMs <= 0L) {
                    bindSessionTiming(_state.value.queue, _state.value.index, running = true)
                } else {
                    seekSessionClock(cardStartMs.getOrElse(_state.value.index) { 0L }, running = true)
                }
                runLoop(gen)
            }
            return
        }
        if (s.paused) {
            _state.update { it.copy(paused = false) }
            tts.resumePlayback()
        } else {
            _state.update { it.copy(paused = true) }
            tts.pausePlayback()
        }
    }

    fun nextCard() {
        val s = _state.value
        if (s.queue.isEmpty()) return
        if (s.index >= s.queue.lastIndex) {
            if (s.prefs.loop) bumpAndRestartFrom(0) else {
                stopInternal(keepQueue = true)
                _state.update { it.copy(status = "done", playing = false) }
            }
            return
        }
        bumpAndRestartFrom(s.index + 1)
    }

    fun prevCard() {
        val s = _state.value
        if (s.queue.isEmpty() || s.index <= 0) return
        bumpAndRestartFrom(s.index - 1)
    }

    fun jumpTo(index: Int) {
        if (index !in _state.value.queue.indices) return
        bumpAndRestartFrom(index)
    }

    fun restartCard() {
        if (_state.value.queue.isEmpty()) return
        bumpAndRestartFrom(_state.value.index)
    }

    fun shuffleNow() {
        val s = _state.value
        if (s.queue.size < 2) return
        val current = s.current
        val shuffled = s.queue.shuffled()
        val idx = shuffled.indexOfFirst { it.id == current?.id }.coerceAtLeast(0)
        generation++
        tts.stop()
        playJob?.cancel()
        _state.update {
            it.copy(
                queue = shuffled,
                index = idx,
                reveal = -1,
                playing = false,
                paused = false,
            )
        }
        viewModelScope.launch {
            bindSessionTiming(shuffled, startIndex = idx, running = false)
        }
    }

    fun stopSession() {
        stopInternal(keepQueue = false)
        _state.update {
            it.copy(
                queue = emptyList(),
                playing = false,
                paused = false,
                reveal = -1,
                status = "",
                editorOpen = false,
                cardElapsedMs = 0L,
                cardDurationMs = 0L,
            )
        }
    }

    fun openEditor() {
        if (_state.value.current == null) return
        _state.update { it.copy(editorOpen = true, paused = true) }
        tts.stop()
    }

    fun closeEditor() = _state.update { it.copy(editorOpen = false) }

    fun saveEditedCard(updated: WordCard) {
        persistCard(updated, restart = true)
        closeEditor()
    }

    fun setCurrentTag(tag: String) {
        val card = _state.value.current ?: return
        val next = tag.trim().ifBlank { "geral" }
        if (card.partOfSpeech.orEmpty() == next) return
        persistCard(card.copy(partOfSpeech = next), restart = true)
    }

    private fun persistCard(updated: WordCard, restart: Boolean) {
        viewModelScope.launch {
            runCatching { repository.updateCard(updated) }
            _state.update { s ->
                s.copy(
                    queue = s.queue.map { if (it.id == updated.id) updated else it },
                    activeCards = s.activeCards.map { if (it.id == updated.id) updated else it },
                )
            }
            if (restart && _state.value.queue.any { it.id == updated.id }) {
                cardStartMs = longArrayOf()
                restartCard()
            }
        }
    }

    private fun persistCloud() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncMessage = null) }
            runCatching { syncService.pushToCloud() }
                .onSuccess { _state.update { it.copy(isSyncing = false, syncMessage = strings().studioDone) } }
                .onFailure {
                    _state.update { it.copy(isSyncing = false, syncMessage = strings().syncErrorGeneric) }
                }
        }
    }

    private fun activeCollection(): StudioCollection? =
        _state.value.collections.find { it.id == _state.value.activeId }
            ?: _state.value.collections.firstOrNull()

    private fun refreshActiveCards() {
        viewModelScope.launch {
            val col = activeCollection()
            val cards = if (col == null) emptyList() else runCatching { syncService.resolveCards(col) }.getOrDefault(emptyList())
            _state.update { it.copy(activeCards = cards) }
        }
    }

    private fun bumpAndRestartFrom(index: Int) {
        generation++
        val gen = generation
        tts.stop()
        playJob?.cancel()
        _state.update {
            it.copy(index = index, reveal = -1, playing = true, paused = false, status = "")
        }
        playJob = viewModelScope.launch {
            if (cardStartMs.isEmpty() || _state.value.cardDurationMs <= 0L) {
                bindSessionTiming(_state.value.queue, index, running = true)
            } else {
                seekSessionClock(cardStartMs.getOrElse(index) { 0L }, running = true)
            }
            runLoop(gen)
        }
    }

    private fun stopInternal(keepQueue: Boolean) {
        generation++
        tts.stop()
        playJob?.cancel()
        playJob = null
        clockHeld = true
        clockRunning = false
        cardFreezeAt = SystemClock.elapsedRealtime()
            if (!keepQueue) {
            tickJob?.cancel()
            tickJob = null
            cardClockStart = 0L
            cardPausedAccum = 0L
            cardFreezeAt = 0L
            clockHeld = false
            cardStartMs = longArrayOf()
        }
        _state.update {
            it.copy(
                playing = false,
                paused = false,
                cardElapsedMs = if (keepQueue) it.cardElapsedMs else 0L,
                cardDurationMs = if (keepQueue) it.cardDurationMs else 0L,
            )
        }
        if (!keepQueue) return
    }

    private suspend fun runLoop(gen: Int) {
        while (gen == generation && _state.value.playing) {
            val s = _state.value
            if (s.index !in s.queue.indices) break
            playCard(s.index, gen)
            if (gen != generation || !_state.value.playing) return
            val cur = _state.value
            if (cur.index >= cur.queue.lastIndex) {
                if (cur.prefs.loop && cur.queue.isNotEmpty()) {
                    val nextQueue = if (cur.prefs.shuffle && cur.queue.size > 1) cur.queue.shuffled() else cur.queue
                    _state.update { it.copy(queue = nextQueue, index = 0, reveal = 0) }
                    if (nextQueue !== cur.queue) {
                        bindSessionTiming(nextQueue, startIndex = 0, running = false)
                    } else {
                        seekSessionClock(0L, running = false)
                    }
                    val pause = cur.prefs.pauseBetweenCardsSec
                    if (pause > 0f) delayWithPause((pause * 1000).toLong(), gen)
                    continue
                }
                seekSessionClock(_state.value.cardDurationMs, running = false)
                _state.update { it.copy(playing = false, status = "done") }
                return
            }
            _state.update { it.copy(index = cur.index + 1, reveal = 0) }
            val pause = cur.prefs.pauseBetweenCardsSec
            if (pause > 0f) delayWithPause((pause * 1000).toLong(), gen)
        }
    }

    private suspend fun playCard(index: Int, gen: Int) {
        if (gen != generation || !_state.value.playing) return
        waitWhilePaused(gen)
        if (gen != generation || !_state.value.playing) return
        val s = _state.value
        val card = s.queue.getOrNull(index) ?: return
        val col = s.collections.find { it.id == s.activeId } ?: s.collections.firstOrNull()
        val hash = col?.render?.hash.orEmpty()
        val sourceIndex = s.activeCards.indexOfFirst { it.id == card.id }.takeIf { it >= 0 } ?: index
        val file = if (col != null && hash.isNotBlank()) tts.cardMp3File(col.id, hash, sourceIndex) else null
        resumeSessionClock()
        _state.update { it.copy(reveal = 3, status = "") }
        val spoken = when {
            file != null && tts.playMp3(file) -> true
            else -> playCardByStages(card, sourceIndex, gen)
        }
        if (!spoken) {
            _state.update { it.copy(playing = false, renderMessage = "need") }
            return
        }
        if (gen == generation && _state.value.playing) {
            intensity?.recordStudio(card)
        }
    }

    private suspend fun playCardByStages(card: WordCard, sourceIndex: Int, gen: Int): Boolean {
        val prefs = _state.value.prefs
        val stages = buildStudioStages(card, sourceIndex + 1, prefs)
        if (stages.isEmpty()) return false
        for ((i, stage) in stages.withIndex()) {
            if (gen != generation || !_state.value.playing) return false
            waitWhilePaused(gen)
            if (gen != generation || !_state.value.playing) return false
            if (stage.reveal >= 0) {
                _state.update { it.copy(reveal = stage.reveal) }
            }
            val ok = tts.speak(
                stage.text,
                stage.lang,
                prefs.speedForKind(stage.kind),
                prefs.voice,
            )
            if (!ok) return false
            if (i != stages.lastIndex) {
                val pause = pauseAfterStage(stage, prefs)
                if (pause > 0f) delayWithPause((pause * 1000).toLong(), gen)
            }
        }
        return gen == generation && _state.value.playing
    }

    private suspend fun bindSessionTiming(
        cards: List<WordCard>,
        startIndex: Int,
        running: Boolean,
    ) {
        val timing = withContext(Dispatchers.IO) {
            val s = _state.value
            val col = s.collections.find { it.id == s.activeId } ?: s.collections.firstOrNull()
            tts.collectionPlayTiming(
                collectionId = col?.id.orEmpty(),
                hash = col?.render?.hash.orEmpty(),
                queue = cards,
                sourceCards = s.activeCards.ifEmpty { cards },
                prefs = s.prefs,
            )
        }
        cardStartMs = timing.startsMs
        _state.update { it.copy(cardDurationMs = timing.totalMs) }
        seekSessionClock(timing.startsMs.getOrElse(startIndex) { 0L }, running)
    }

    private fun seekSessionClock(elapsedMs: Long, running: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = elapsedMs.coerceAtLeast(0L)
        cardClockStart = now - elapsed
        cardPausedAccum = 0L
        clockHeld = !running
        clockRunning = running && !_state.value.paused
        cardFreezeAt = if (clockRunning) 0L else now
        _state.update { it.copy(cardElapsedMs = elapsed) }
        ensureCardTicker()
    }

    private fun resumeSessionClock() {
        clockHeld = false
        val now = SystemClock.elapsedRealtime()
        if (!_state.value.paused && _state.value.playing) {
            if (!clockRunning && cardFreezeAt > 0L) {
                cardPausedAccum += now - cardFreezeAt
                cardFreezeAt = 0L
            }
            clockRunning = true
        }
        ensureCardTicker()
    }

    private fun currentCardElapsed(): Long {
        val now = if (clockRunning) {
            SystemClock.elapsedRealtime()
        } else {
            cardFreezeAt.takeIf { it > 0L } ?: cardClockStart
        }
        return (now - cardClockStart - cardPausedAccum).coerceAtLeast(0L)
    }

    private fun syncClockPaused(paused: Boolean) {
        if (clockHeld) return
        val now = SystemClock.elapsedRealtime()
        if (paused && clockRunning) {
            clockRunning = false
            cardFreezeAt = now
        } else if (!paused && !clockRunning && cardFreezeAt > 0L && _state.value.playing) {
            cardPausedAccum += now - cardFreezeAt
            cardFreezeAt = 0L
            clockRunning = true
        }
    }

    private fun ensureCardTicker() {
        if (tickJob?.isActive == true) return
        tickJob = viewModelScope.launch {
            while (_state.value.sessionOpen) {
                val s = _state.value
                syncClockPaused(s.paused || !s.playing)
                val raw = currentCardElapsed()
                val cap = s.cardDurationMs
                val elapsed = if (cap > 0L) raw.coerceAtMost(cap) else raw
                if (elapsed != s.cardElapsedMs) {
                    _state.update { it.copy(cardElapsedMs = elapsed) }
                }
                delay(200)
            }
        }
    }

    private suspend fun waitWhilePaused(gen: Int) {
        while (_state.value.paused && _state.value.playing && gen == generation) delay(120)
    }

    private suspend fun delayWithPause(ms: Long, gen: Int) {
        var left = ms
        while (left > 0 && gen == generation && _state.value.playing) {
            waitWhilePaused(gen)
            if (gen != generation || !_state.value.playing) return
            val step = minOf(200L, left)
            delay(step)
            left -= step
            if (left > 0) {
                val sec = ((left + 999) / 1000).toInt().coerceAtLeast(1)
                _state.update { it.copy(status = "next:$sec") }
            }
        }
        _state.update { it.copy(status = "") }
    }

    override fun onCleared() {
        StudioNowPlayingHub.unbind(playbackCommands)
        StudioNowPlayingHub.clear()
        stopInternal(keepQueue = false)
        tts.shutdown()
        super.onCleared()
    }
}

class StudioViewModelFactory(
    private val repository: ProfconqRepository,
    private val store: StudioStore,
    private val syncService: StudioSyncService,
    private val tts: StudioTtsPlayer,
    private val intensity: com.profconq.app.study.StudyIntensityStore? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudioViewModel::class.java)) {
            return StudioViewModel(repository, store, syncService, tts, intensity) as T
        }
        error("Unknown ViewModel")
    }
}
