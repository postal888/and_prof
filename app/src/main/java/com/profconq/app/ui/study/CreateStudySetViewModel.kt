package com.profconq.app.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.profconq.app.data.model.WordCard
import com.profconq.app.data.repository.ProfconqRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CreateStudySetUiState(
    val name: String = defaultName(),
    val selectedWords: List<WordCard> = emptyList(),
) {
    companion object {
        fun defaultName(): String {
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return "Набор от ${formatter.format(Date())}"
        }
    }
}

class CreateStudySetViewModel(
    private val repository: ProfconqRepository,
    private val selectionStore: StudyWordSelectionStore,
) : ViewModel() {
    private val _name = MutableStateFlow(CreateStudySetUiState.defaultName())

    val uiState: StateFlow<CreateStudySetUiState> = combine(
        _name,
        selectionStore.selectedIds,
    ) { name, ids ->
        CreateStudySetUiState(
            name = name,
            selectedWords = emptyList(), // filled async below
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreateStudySetUiState())

    private val _words = MutableStateFlow<List<WordCard>>(emptyList())

    val state: StateFlow<CreateStudySetUiState> = combine(_name, _words) { name, words ->
        CreateStudySetUiState(name = name, selectedWords = words)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreateStudySetUiState())

    init {
        viewModelScope.launch {
            selectionStore.selectedIds.collect { ids ->
                _words.value = repository.getCardsByIds(ids.toList())
            }
        }
    }

    fun setName(name: String) {
        _name.value = name
    }

    fun removeWord(wordId: String) {
        selectionStore.toggle(wordId)
    }

    fun createSet(onCreated: (String) -> Unit) {
        val ids = selectionStore.selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val id = repository.createStudySet(_name.value, ids)
            selectionStore.clear()
            onCreated(id)
        }
    }

    fun syncFromStore() {
        viewModelScope.launch {
            _words.value = repository.getCardsByIds(selectionStore.selectedIds.value.toList())
        }
    }
}

class CreateStudySetViewModelFactory(
    private val repository: ProfconqRepository,
    private val selectionStore: StudyWordSelectionStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateStudySetViewModel::class.java)) {
            return CreateStudySetViewModel(repository, selectionStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
