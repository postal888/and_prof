package com.profconq.app.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudySetSettingsUiState(
    val setName: String = "",
    val words: List<WordCard> = emptyList(),
)

class StudySetSettingsViewModel(
    private val repository: ProfconqRepository,
    private val setId: String,
) : ViewModel() {
    private val _setName = MutableStateFlow("")
    val setName: StateFlow<String> = _setName.asStateFlow()

    val words: StateFlow<List<WordCard>> =
        repository.observeWordsInSet(setId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _setName.value = repository.getStudySet(setId)?.name.orEmpty()
        }
    }

    fun removeWord(wordId: String) {
        viewModelScope.launch {
            repository.removeWordFromStudySet(setId, wordId)
        }
    }

    fun addWords(wordIds: Set<String>) {
        viewModelScope.launch {
            repository.addWordsToStudySet(setId, wordIds.toList())
        }
    }
}

class StudySetSettingsViewModelFactory(
    private val repository: ProfconqRepository,
    private val setId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudySetSettingsViewModel::class.java)) {
            return StudySetSettingsViewModel(repository, setId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
