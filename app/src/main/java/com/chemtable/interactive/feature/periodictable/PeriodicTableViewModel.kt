package com.chemtable.interactive.feature.periodictable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PeriodicTableViewModel @Inject constructor(
    getElementsUseCase: GetElementsUseCase
) : ViewModel() {

    val elements: StateFlow<List<Element>> = getElementsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
