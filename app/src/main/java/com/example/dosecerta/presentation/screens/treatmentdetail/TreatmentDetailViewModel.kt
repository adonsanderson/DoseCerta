package com.example.dosecerta.presentation.screens.treatmentdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosecerta.domain.scheduler.TimeProvider
import com.example.dosecerta.domain.usecase.DeleteTreatmentUseCase
import com.example.dosecerta.domain.usecase.ObserveTreatmentDetailUseCase
import com.example.dosecerta.domain.usecase.ToggleDoseStatusUseCase
import com.example.dosecerta.presentation.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TreatmentDetailViewModel(
    private val treatmentId: Long,
    private val observeTreatmentDetailUseCase: ObserveTreatmentDetailUseCase,
    private val toggleDoseStatusUseCase: ToggleDoseStatusUseCase,
    private val deleteTreatmentUseCase: DeleteTreatmentUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TreatmentDetailUiState(
            contentState = UiState.Loading,
            nowMillis = timeProvider.nowMillis()
        )
    )
    val uiState: StateFlow<TreatmentDetailUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null

    init {
        observeTreatment()
    }

    fun observeTreatment() {
        observationJob?.cancel()
        _uiState.update { it.copy(contentState = UiState.Loading) }

        observationJob = viewModelScope.launch {
            observeTreatmentDetailUseCase(treatmentId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            contentState = UiState.Error(
                                message = error.message
                                    ?: "Não foi possível carregar os detalhes do tratamento.",
                                canRetry = true
                            )
                        )
                    }
                }
                .collect { detail ->
                    val currentNow = timeProvider.nowMillis()
                    val nextState = if (detail == null) {
                        UiState.Empty("Tratamento não encontrado.")
                    } else {
                        UiState.Success(detail)
                    }
                    _uiState.update {
                        it.copy(
                            contentState = nextState,
                            nowMillis = currentNow
                        )
                    }
                }
        }
    }

    fun onToggleDoseStatus(doseId: Long) {
        viewModelScope.launch {
            toggleDoseStatusUseCase(doseId).onFailure { error ->
                _uiState.update {
                    it.copy(
                        feedbackMessage = error.message
                            ?: "Não foi possível atualizar o status da dose."
                    )
                }
            }
        }
    }

    fun onDeleteTreatment() {
        if (_uiState.value.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            deleteTreatmentUseCase(treatmentId)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            feedbackMessage = error.message
                                ?: "Não foi possível excluir o tratamento."
                        )
                    }
                }
        }
    }

    fun onDismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
