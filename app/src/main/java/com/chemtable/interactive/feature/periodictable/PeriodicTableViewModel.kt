package com.chemtable.interactive.feature.periodictable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.util.StartupTrace
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class PeriodicTableViewModel @Inject constructor(
    private val getElementsUseCaseProvider: Provider<GetElementsUseCase>
) : ViewModel() {
    private val _elements = MutableStateFlow<List<Element>>(emptyList())
    val elements: StateFlow<List<Element>> = _elements.asStateFlow()

    init {
        // Collect elements off the main thread for the whole ViewModel lifetime. Unlike
        // stateIn(WhileSubscribed), the backing MutableStateFlow always replays the latest
        // list to new collectors, so late UI subscribers never observe a stale/empty value;
        // while the DB is still seeding the flow stays empty and the screen shows its loading pane.
        StartupTrace.mark("PeriodicTableViewModel init")
        viewModelScope.launch(Dispatchers.IO) {
            StartupTrace.mark("PeriodicTableViewModel element collection started")
            getElementsUseCaseProvider.get()().collect { elements ->
                StartupTrace.mark("PeriodicTableViewModel elements emitted count=${elements.size}")
                _elements.value = elements
            }
        }
    }
}
