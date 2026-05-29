package com.proficon.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proficon.app.data.model.AppSettings
import com.proficon.app.data.model.AppThemeMode
import com.proficon.app.data.model.Collection
import com.proficon.app.data.model.DictionaryEntry
import com.proficon.app.data.model.ProgressSnapshot
import com.proficon.app.data.model.TodayPlan
import com.proficon.app.data.model.WordCard
import com.proficon.app.data.repository.ProficonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ProficonRepository,
) : ViewModel() {
    val collections: StateFlow<List<Collection>> =
        repository.collections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dictionaryCollections: StateFlow<List<Collection>> =
        repository.dictionaryCollections.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dictionary: StateFlow<List<DictionaryEntry>> =
        repository.dictionary.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayPlan: StateFlow<TodayPlan> =
        repository.todayPlan.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayPlan(dueCount = 0, newCount = 0, streak = 0),
        )

    val progress: StateFlow<ProgressSnapshot> =
        repository.progressSnapshot.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ProgressSnapshot(),
        )

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings(),
        )

    fun setUseChatGptTranslation(enabled: Boolean) {
        viewModelScope.launch { repository.setUseChatGptTranslation(enabled) }
    }

    fun setPhraseCopyEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPhraseCopyEnabled(enabled) }
    }

    fun setWordContextExampleEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setWordContextExampleEnabled(enabled) }
    }

    fun setSubtitleFontSizeLevel(level: Int) {
        viewModelScope.launch { repository.setSubtitleFontSizeLevel(level) }
    }

    fun setUiLanguage(languageCode: Int) {
        viewModelScope.launch { repository.setUiLanguage(languageCode) }
    }

    fun setSubtitleLanguage(languageCode: Int) {
        viewModelScope.launch { repository.setSubtitleLanguage(languageCode) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun markKnown(cardId: String) {
        viewModelScope.launch { repository.markCardKnown(cardId) }
    }

    fun markRepeat(cardId: String) {
        viewModelScope.launch { repository.markCardRepeat(cardId) }
    }

    fun deleteCollection(collectionId: String) {
        viewModelScope.launch { repository.deleteCollection(collectionId) }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch { repository.deleteCard(cardId) }
    }

    fun updateCollectionTitle(collectionId: String, title: String) {
        viewModelScope.launch { repository.updateCollectionTitle(collectionId, title) }
    }

    fun updateCollectionDescription(collectionId: String, description: String?) {
        viewModelScope.launch { repository.updateCollectionDescription(collectionId, description) }
    }

    fun createManualCollection(title: String, description: String? = null, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createManualCollection(title, description)
            onCreated(id)
        }
    }

    fun addCardToCollection(collectionId: String, pt: String, ru: String, example: String? = null) {
        viewModelScope.launch { repository.addCardToCollection(collectionId, pt, ru, example) }
    }

    fun updateCollectionFolderAudio(collectionId: String, path: String?) {
        viewModelScope.launch { repository.updateCollectionFolderAudio(collectionId, path) }
    }

    fun updateCard(card: WordCard) {
        viewModelScope.launch { repository.updateCard(card) }
    }

    fun setCardLearnMark(cardId: String, mark: String) {
        viewModelScope.launch {
            when (mark) {
                "good" -> repository.markCardKnown(cardId)
                "medium" -> repository.markCardLearning(cardId)
                else -> repository.markCardRepeat(cardId)
            }
        }
    }

    fun addDictionaryEntry(pt: String, ru: String, example: String? = null) {
        viewModelScope.launch { repository.addDictionaryEntry(pt, ru, example) }
    }

    fun resetProgressMetrics() {
        viewModelScope.launch { repository.resetProgressMetrics() }
    }
}

class MainViewModelFactory(
    private val repository: ProficonRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
