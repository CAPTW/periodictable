package com.chemtable.interactive.feature.minigame.reactor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemtable.interactive.feature.minigame.reactor.economy.ReactorSupplyAccount
import com.chemtable.interactive.feature.minigame.reactor.economy.ReactorSupplyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReactorSupplyUiState(
    val quantity: Int? = null,
    val claimed: Boolean = false,
    val busy: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class ReactorSupplyViewModel @Inject constructor(
    private val repository: ReactorSupplyRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReactorSupplyUiState())
    val state = mutableState.asStateFlow()

    init { reload() }

    fun reload() = perform { repository.read() }

    fun claim() {
        val current = mutableState.value
        if (current.claimed || current.quantity == null || current.error) return
        perform { repository.claim() }
    }

    private fun perform(operation: suspend () -> ReactorSupplyAccount) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, error = false)
        viewModelScope.launch {
            try {
                val account = operation()
                mutableState.value = ReactorSupplyUiState(account.quantity, account.claimed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Unknown balance is not displayed as zero or reported as a successful claim.
                mutableState.value = ReactorSupplyUiState(error = true)
            }
        }
    }
}
