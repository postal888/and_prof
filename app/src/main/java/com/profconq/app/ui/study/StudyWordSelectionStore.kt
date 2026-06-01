package com.profconq.app.ui.study

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Shared selection state between Create Study Set and Dictionary selection mode. */
class StudyWordSelectionStore {
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun set(ids: Set<String>) {
        _selectedIds.value = ids
    }

    fun toggle(wordId: String) {
        _selectedIds.update { current ->
            if (wordId in current) current - wordId else current + wordId
        }
    }

    fun setFolder(cardIds: List<String>, selectAll: Boolean) {
        _selectedIds.update { current ->
            if (selectAll) current + cardIds else current - cardIds.toSet()
        }
    }

    fun clear() {
        _selectedIds.value = emptySet()
    }
}
