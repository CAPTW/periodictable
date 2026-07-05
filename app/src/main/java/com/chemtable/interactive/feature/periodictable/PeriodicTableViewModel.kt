package com.chemtable.interactive.feature.periodictable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.core.model.AppSettings
import com.chemtable.interactive.core.model.Element
import com.chemtable.interactive.core.model.TableViewMode
import com.chemtable.interactive.domain.repository.SettingsRepository
import com.chemtable.interactive.domain.usecase.GetElementsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PeriodicTableViewModel @Inject constructor(
    getElementsUseCase: GetElementsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val elements: StateFlow<List<Element>> = getElementsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Whether the table is shown fitted to one screen or in a pan/zoom canvas. Persisted preference. */
    val tableViewMode: StateFlow<TableViewMode> = settingsRepository.settings
        .map { it.tableViewMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings.DEFAULT.tableViewMode)

    fun setTableViewMode(mode: TableViewMode) {
        viewModelScope.launch { settingsRepository.setTableViewMode(mode) }
    }
}
