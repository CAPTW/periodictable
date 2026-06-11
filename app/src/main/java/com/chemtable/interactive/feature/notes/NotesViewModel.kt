package com.chemtable.interactive.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.ElementNote
import com.chemtable.interactive.domain.usecase.ManageNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel @Inject constructor(
    private val manageNotesUseCase: ManageNotesUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val elementFilter = MutableStateFlow<Int?>(null)

    val notes: Flow<List<ElementNote>> = combine(searchQuery, elementFilter) { query, elementId ->
        val currentQuery = query.trim()
        Pair(currentQuery, elementId)
    }.flatMapLatest { (query, elementId) ->
        val source: Flow<List<ElementNote>> = if (elementId == null) {
            manageNotesUseCase.getAllNotes()
        } else {
            manageNotesUseCase.getNotesForElement(elementId)
        }
        if (query.isBlank()) {
            source
        } else {
            source.map { notes ->
                notes.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true) ||
                        note.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
        }
    }

    val notesUiState = notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setElementFilter(elementId: Int?) {
        elementFilter.value = elementId.takeIf { it != null && it > 0 }
    }

    fun deleteNote(note: ElementNote) {
        viewModelScope.launch {
            manageNotesUseCase.delete(note)
        }
    }
}
