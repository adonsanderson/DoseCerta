package com.example.dosecerta.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.scheduler.TimeProvider
import com.example.dosecerta.domain.usecase.ObserveTodayDosesUseCase
import com.example.dosecerta.domain.usecase.ObserveTreatmentsUseCase
import com.example.dosecerta.domain.usecase.ToggleDoseStatusUseCase
import com.example.dosecerta.presentation.common.DateTimeFormatters
import com.example.dosecerta.presentation.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeTreatmentsUseCase: ObserveTreatmentsUseCase,
    private val observeTodayDosesUseCase: ObserveTodayDosesUseCase,
    private val toggleDoseStatusUseCase: ToggleDoseStatusUseCase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(contentState = UiState.Loading))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var observationJob: Job? = null

    init {
        observeSchedule()
    }

    fun observeSchedule() {
        observationJob?.cancel()
        _uiState.update { it.copy(contentState = UiState.Loading) }

        val nowMillis = timeProvider.nowMillis()
        val (dayStartMillis, dayEndMillis) = DateTimeFormatters.todayBoundsMillis(nowMillis)

        observationJob = viewModelScope.launch {
            combine(
                observeTreatmentsUseCase(),
                observeTodayDosesUseCase(dayStartMillis, dayEndMillis)
            ) { treatments, todayDoses ->
                val currentNow = timeProvider.nowMillis()
                if (treatments.isEmpty()) {
                    UiState.Empty("Nenhum tratamento cadastrado ainda.")
                } else {
                    val todayAdherence = AdherenceSummary.fromDoses(todayDoses, currentNow)
                    UiState.Success(
                        HomeContent(
                            todayDoses = todayDoses,
                            treatments = treatments,
                            todayAdherence = todayAdherence,
                            nowMillis = currentNow
                        )
                    )
                }
            }.catch { error ->
                _uiState.update {
                    it.copy(
                        contentState = UiState.Error(
                            message = error.message
                                ?: "Não foi possível carregar o cronograma de medicamentos.",
                            canRetry = true
                        )
                    )
                }
            }.collect { nextContentState ->
                _uiState.update { it.copy(contentState = nextContentState) }
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

    fun onDismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
