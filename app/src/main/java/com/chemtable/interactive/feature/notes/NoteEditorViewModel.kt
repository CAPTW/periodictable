package com.chemtable.interactive.feature.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.ElementNote
import com.chemtable.interactive.domain.usecase.ManageNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val manageNotesUseCase: ManageNotesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId = savedStateHandle.get<Long>("noteId")
    private val rawElementId = savedStateHandle.get<Int>("elementId")
    val elementId: Int? = rawElementId?.takeIf { it > 0 }

    private val _noteUiState = MutableStateFlow(
        NoteEditorUiState(
            noteId = noteId?.takeIf { it > 0 },
            elementId = elementId,
            title = "",
            content = "",
            tagsText = "",
            createdAt = System.currentTimeMillis()
        )
    )
    val noteUiState: StateFlow<NoteEditorUiState> = _noteUiState.asStateFlow()
    private var loadedNote: ElementNote? = null

    init {
        if ((noteId ?: 0L) > 0L) {
            loadExistingNote()
        }
    }

    private fun loadExistingNote() {
        val targetId = noteId ?: return
        viewModelScope.launch {
            val found = manageNotesUseCase.getNoteById(targetId)
                .firstOrNull()

            found?.let { note ->
                loadedNote = note
                _noteUiState.update {
                    it.copy(
                        noteId = note.id,
                        elementId = note.elementAtomicNumber,
                        title = note.title,
                        content = note.content,
                        tagsText = note.tags.joinToString(", "),
                        createdAt = note.createdAt
                    )
                }
            }
        }
    }

    fun onTitleChanged(value: String) {
        _noteUiState.update { it.copy(title = value) }
    }

    fun onContentChanged(value: String) {
        _noteUiState.update { it.copy(content = value) }
    }

    fun onTagsChanged(value: String) {
        _noteUiState.update { it.copy(tagsText = value) }
    }

    fun saveNote(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val state = noteUiState.value
            if (state.title.isBlank() && state.content.isBlank()) return@launch

            val now = System.currentTimeMillis()
            val tags = state.tagsText
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val note = ElementNote(
                id = state.noteId ?: 0L,
                elementAtomicNumber = state.elementId ?: 0,
                title = state.title.ifBlank { "Untitled" },
                content = state.content,
                tags = tags,
                createdAt = state.createdAt,
                updatedAt = now
            )

            manageNotesUseCase.upsert(note)
            loadedNote = note
            onSaved()
        }
    }

    fun deleteCurrentNote(onDeleted: () -> Unit = {}) {
        val target = loadedNote ?: return
        viewModelScope.launch {
            manageNotesUseCase.delete(target)
            onDeleted()
        }
    }
}

data class NoteEditorUiState(
    val noteId: Long?,
    val elementId: Int?,
    val title: String,
    val content: String,
    val tagsText: String,
    val createdAt: Long = System.currentTimeMillis()
)
