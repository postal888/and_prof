package com.profconq.app.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque

data class PracticeSessionUiState(
    val isLoading: Boolean = true,
    val setName: String = "",
    val setTotalWords: Int = 0,
    val queue: List<WordCard> = emptyList(),
    val direction: StudyDirection = StudyDirection.PT_TO_RU,
    val isFlipped: Boolean = false,
    val isExampleExpanded: Boolean = false,
    val initialTotal: Int = 0,
    val knownCount: Int = 0,
    val dontKnowCount: Int = 0,
    val deckMasteredCount: Int = 0,
    val isComplete: Boolean = false,
    val isAllReviewedToday: Boolean = false,
    val isFreeReview: Boolean = false,
    val showExitDialog: Boolean = false,
    val collectionTitles: Map<String, String> = emptyMap(),
) {
    val current: WordCard? get() = queue.firstOrNull()
    val remaining: Int get() = queue.size
    val reviewedCount: Int
        get() = if (isComplete) initialTotal else (initialTotal - queue.size).coerceIn(0, initialTotal)
    val currentCardNumber: Int
        get() {
            if (isComplete || initialTotal == 0) return 0
            return reviewedCount.coerceIn(0, initialTotal - 1) + 1
        }
    val progressPercent: Int
        get() {
            if (initialTotal == 0) return 0
            if (isComplete) return 100
            return (reviewedCount * 100 / initialTotal).coerceIn(0, 100)
        }

    val lessonWordCount: Int get() = initialTotal
}

class PracticeSessionViewModel(
    private val repository: ProfconqRepository,
    private val setId: String,
    private val startWithFullSet: Boolean = false,
    private val intensity: com.profconq.app.study.StudyIntensityStore? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(PracticeSessionUiState())
    val state: StateFlow<PracticeSessionUiState> = _state.asStateFlow()

    private var queueDeque = ArrayDeque<WordCard>()
    private val knownCardIds = mutableSetOf<String>()
    private val dontKnowCardIds = mutableSetOf<String>()
    private var lessonFinished = false

    init {
        viewModelScope.launch {
            repository.markStudySetPracticed(setId)
            val set = repository.getStudySet(setId)
            val now = System.currentTimeMillis()
            val stats = repository.getStudySetReviewStats(setId, now)
            val collections = repository.collections.first()
            val titles = collections.associate { it.id to it.title }
            val words = if (startWithFullSet) {
                repository.getAllWordsForStudySet(setId)
            } else {
                repository.getDueWordsForStudySet(setId, now)
            }

            when {
                words.isEmpty() && !startWithFullSet -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isAllReviewedToday = true,
                            setName = set?.name.orEmpty(),
                            setTotalWords = set?.wordCount ?: 0,
                            deckMasteredCount = stats.masteredCount,
                        )
                    }
                }
                words.isEmpty() -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isAllReviewedToday = true,
                            setName = set?.name.orEmpty(),
                            setTotalWords = 0,
                            deckMasteredCount = stats.masteredCount,
                        )
                    }
                }
                else -> {
                    beginSession(
                        words = words,
                        setName = set?.name.orEmpty(),
                        setTotalWords = set?.wordCount ?: words.size,
                        collectionTitles = titles,
                        freeReview = startWithFullSet,
                    )
                }
            }
        }
    }

    fun startFullSetReview() {
        viewModelScope.launch {
            val set = repository.getStudySet(setId)
            val words = repository.getAllWordsForStudySet(setId)
            val collections = repository.collections.first()
            val titles = collections.associate { it.id to it.title }
            if (words.isEmpty()) return@launch
            beginSession(
                words = words,
                setName = set?.name.orEmpty(),
                setTotalWords = set?.wordCount ?: words.size,
                collectionTitles = titles,
                freeReview = true,
            )
        }
    }

    private fun beginSession(
        words: List<WordCard>,
        setName: String,
        setTotalWords: Int,
        collectionTitles: Map<String, String>,
        freeReview: Boolean,
    ) {
        lessonFinished = false
        knownCardIds.clear()
        dontKnowCardIds.clear()
        queueDeque = ArrayDeque(words)
        _state.update {
            it.copy(
                isLoading = false,
                isAllReviewedToday = false,
                isFreeReview = freeReview,
                setName = setName,
                setTotalWords = setTotalWords,
                queue = queueDeque.toList(),
                initialTotal = words.size,
                collectionTitles = collectionTitles,
                isComplete = false,
                isFlipped = false,
                isExampleExpanded = false,
                knownCount = 0,
                dontKnowCount = 0,
            )
        }
    }

    fun showExitDialog() {
        _state.update { it.copy(showExitDialog = true) }
    }

    fun dismissExitDialog() {
        _state.update { it.copy(showExitDialog = false) }
    }

    fun toggleDirection() {
        _state.update {
            it.copy(direction = it.direction.opposite(), isFlipped = false, isExampleExpanded = false)
        }
    }

    fun flipCard() {
        _state.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun toggleExample() {
        _state.update { it.copy(isExampleExpanded = !it.isExampleExpanded) }
    }

    fun rateKnown() {
        if (lessonFinished || queueDeque.isEmpty()) return
        val current = queueDeque.removeFirst()
        knownCardIds.add(current.id)
        advanceAfterRating(current, isKnown = true)
    }

    fun rateDontKnow() {
        if (lessonFinished || queueDeque.isEmpty()) return
        val current = queueDeque.removeFirst()
        dontKnowCardIds.add(current.id)
        advanceAfterRating(current, isKnown = false)
    }

    private fun advanceAfterRating(card: WordCard, isKnown: Boolean) {
        val wordId = card.id
        val finished = queueDeque.isEmpty()
        if (finished) lessonFinished = true
        _state.update { s ->
            s.copy(
                queue = queueDeque.toList(),
                isFlipped = false,
                isExampleExpanded = false,
                knownCount = knownCardIds.size,
                dontKnowCount = dontKnowCardIds.size,
                isComplete = finished,
            )
        }
        viewModelScope.launch {
            intensity?.recordCard(card, known = isKnown)
            if (isKnown) {
                repository.recordReviewKnown(wordId)
            } else {
                repository.recordReviewAgain(wordId)
            }
            if (finished) {
                repository.retainOnlyWordsInStudySet(setId, dontKnowCardIds)
                val stats = repository.getStudySetReviewStats(setId)
                val set = repository.getStudySet(setId)
                _state.update {
                    it.copy(
                        deckMasteredCount = stats.masteredCount,
                        setTotalWords = set?.wordCount ?: dontKnowCardIds.size,
                    )
                }
            }
        }
    }

    fun toggleFavorite(cardId: String) {
        viewModelScope.launch {
            repository.toggleCardFavorite(cardId)
            val updated = repository.getCardsByIds(listOf(cardId)).firstOrNull() ?: return@launch
            queueDeque = ArrayDeque(queueDeque.map { if (it.id == cardId) updated else it })
            _state.update { s -> s.copy(queue = queueDeque.toList()) }
        }
    }

    fun removeCurrentFromSet() {
        val current = queueDeque.firstOrNull() ?: return
        viewModelScope.launch {
            repository.removeWordFromStudySet(setId, current.id)
            knownCardIds.remove(current.id)
            dontKnowCardIds.remove(current.id)
            queueDeque.removeFirst()
            val finished = queueDeque.isEmpty()
            if (finished) lessonFinished = true
            _state.update { s ->
                val newInitial = (s.initialTotal - 1).coerceAtLeast(0)
                s.copy(
                    queue = queueDeque.toList(),
                    initialTotal = newInitial,
                    setTotalWords = (s.setTotalWords - 1).coerceAtLeast(0),
                    knownCount = knownCardIds.size,
                    dontKnowCount = dontKnowCardIds.size,
                    isFlipped = false,
                    isExampleExpanded = false,
                    isComplete = finished,
                )
            }
            if (finished) {
                repository.retainOnlyWordsInStudySet(setId, dontKnowCardIds)
            }
        }
    }

    fun collectionTitleFor(card: WordCard): String? =
        _state.value.collectionTitles[card.collectionId]
}

class PracticeSessionViewModelFactory(
    private val repository: ProfconqRepository,
    private val setId: String,
    private val startWithFullSet: Boolean = false,
    private val intensity: com.profconq.app.study.StudyIntensityStore? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeSessionViewModel::class.java)) {
            return PracticeSessionViewModel(repository, setId, startWithFullSet, intensity) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
