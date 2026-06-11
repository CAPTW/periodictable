package com.chemtable.interactive.feature.glossary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.GlossaryTerm
import com.chemtable.interactive.domain.usecase.GetGlossaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlossaryViewModel @Inject constructor(
    private val getGlossaryUseCase: GetGlossaryUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedTermId = MutableStateFlow<String?>(null)

    private val allTerms: StateFlow<List<GlossaryTerm>> = getGlossaryUseCase.allTerms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val filteredTerms: StateFlow<List<GlossaryTerm>> = combine(allTerms, _query) { terms, query ->
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            terms
        } else {
            terms.filter { term ->
                term.termKo.contains(trimmed, ignoreCase = true) ||
                    term.termEn.contains(trimmed, ignoreCase = true) ||
                    term.definition.contains(trimmed, ignoreCase = true) ||
                    term.simpleExplanation.contains(trimmed, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val selectedTerm: StateFlow<GlossaryTerm?> = combine(allTerms, _selectedTermId) { terms, id ->
        id?.let { targetId -> terms.firstOrNull { it.id == targetId } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val relatedTerms: StateFlow<List<GlossaryTerm>> = combine(allTerms, selectedTerm) { terms, selected ->
        val relatedIds = selected?.relatedTerms ?: emptyList()
        relatedIds.mapNotNull { id -> terms.firstOrNull { it.id == id } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun selectTerm(termId: String) {
        _selectedTermId.value = termId
    }

    fun toggleBookmark(term: GlossaryTerm) {
        viewModelScope.launch {
            getGlossaryUseCase.setBookmark(term.id, !term.isBookmarked)
        }
    }
}
