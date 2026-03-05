package com.chemtable.interactive.feature.elementdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.domain.usecase.GetElementDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ElementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getElementDetailUseCase: GetElementDetailUseCase
) : ViewModel() {
    private val atomicNumber: Int = checkNotNull(savedStateHandle["atomicNumber"])

    val element: StateFlow<Element?> = getElementDetailUseCase(atomicNumber).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )
}
