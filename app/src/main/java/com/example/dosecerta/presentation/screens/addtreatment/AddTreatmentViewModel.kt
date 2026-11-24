package com.example.dosecerta.presentation.screens.addtreatment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosecerta.domain.scheduler.TimeProvider
import com.example.dosecerta.domain.usecase.CalculateDoseScheduleUseCase
import com.example.dosecerta.domain.usecase.CreateTreatmentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddTreatmentViewModel(
    private val calculateDoseScheduleUseCase: CalculateDoseScheduleUseCase,
    private val createTreatmentUseCase: CreateTreatmentUseCase,
    timeProvider: TimeProvider
) : ViewModel() {

    private val initialStartMillis = roundUpToNextTenMinutes(timeProvider.nowMillis())

    private val _uiState = MutableStateFlow(
        AddTreatmentUiState(
            startTimeMillis = initialStartMillis,
            previewDoseCount = computePreviewCount(
                startTimeMillis = initialStartMillis,
                intervalHoursText = "8",
                durationDaysText = "7"
            )
        )
    )
    val uiState: StateFlow<AddTreatmentUiState> = _uiState.asStateFlow()

    fun onMedicationNameChange(value: String) {
        _uiState.update {
            it.copy(
                medicationName = value,
                medicationNameError = null,
                errorMessage = null
            )
        }
    }

    fun onDosageChange(value: String) {
        _uiState.update { it.copy(dosage = value, errorMessage = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value, errorMessage = null) }
    }

    fun onStartTimeChange(startTimeMillis: Long) {
        _uiState.update { current ->
            current.copy(
                startTimeMillis = startTimeMillis,
                previewDoseCount = computePreviewCount(
                    startTimeMillis = startTimeMillis,
                    intervalHoursText = current.intervalHoursText,
                    durationDaysText = current.durationDaysText
                ),
                errorMessage = null
            )
        }
    }

    fun onIntervalHoursChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(3)
        _uiState.update { current ->
            current.copy(
                intervalHoursText = digitsOnly,
                intervalError = null,
                previewDoseCount = computePreviewCount(
                    startTimeMillis = current.startTimeMillis,
                    intervalHoursText = digitsOnly,
                    durationDaysText = current.durationDaysText
                ),
                errorMessage = null
            )
        }
    }

    fun onDurationDaysChange(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(3)
        _uiState.update { current ->
            current.copy(
                durationDaysText = digitsOnly,
                durationError = null,
                previewDoseCount = computePreviewCount(
                    startTimeMillis = current.startTimeMillis,
                    intervalHoursText = current.intervalHoursText,
                    durationDaysText = digitsOnly
                ),
                errorMessage = null
            )
        }
    }

    fun onSaveTreatment() {
        val current = _uiState.value
        if (current.isSaving) return

        val nameError = if (current.medicationName.isBlank()) {
            "Informe o nome do medicamento."
        } else {
            null
        }

        val intervalHours = current.intervalHoursText.toIntOrNull()
        val intervalError = if (
            intervalHours == null ||
            intervalHours !in CalculateDoseScheduleUseCase.MIN_INTERVAL_HOURS..CalculateDoseScheduleUseCase.MAX_INTERVAL_HOURS
        ) {
            "Use um intervalo entre ${CalculateDoseScheduleUseCase.MIN_INTERVAL_HOURS} e ${CalculateDoseScheduleUseCase.MAX_INTERVAL_HOURS}h."
        } else {
            null
        }

        val durationDays = current.durationDaysText.toIntOrNull()
        val durationError = if (
            durationDays == null ||
            durationDays !in CalculateDoseScheduleUseCase.MIN_DURATION_DAYS..CalculateDoseScheduleUseCase.MAX_DURATION_DAYS
        ) {
            "Use uma duração entre ${CalculateDoseScheduleUseCase.MIN_DURATION_DAYS} e ${CalculateDoseScheduleUseCase.MAX_DURATION_DAYS} dias."
        } else {
            null
        }

        if (nameError != null || intervalError != null || durationError != null) {
            _uiState.update {
                it.copy(
                    medicationNameError = nameError,
                    intervalError = intervalError,
                    durationError = durationError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            createTreatmentUseCase(
                medicationName = current.medicationName,
                dosage = current.dosage,
                notes = current.notes,
                startTimeMillis = current.startTimeMillis,
                intervalHours = intervalHours!!,
                durationDays = durationDays!!
            ).onSuccess { treatmentId ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedTreatmentId = treatmentId
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message
                            ?: "Não foi possível salvar o tratamento."
                    )
                }
            }
        }
    }

    private fun computePreviewCount(
        startTimeMillis: Long,
        intervalHoursText: String,
        durationDaysText: String
    ): Int? {
        val interval = intervalHoursText.toIntOrNull() ?: return null
        val duration = durationDaysText.toIntOrNull() ?: return null
        return runCatching {
            calculateDoseScheduleUseCase(
                startTimeMillis = startTimeMillis,
                intervalHours = interval,
                durationDays = duration
            ).size
        }.getOrNull()
    }

    companion object {
        private const val TEN_MINUTES_MILLIS = 600_000L

        private fun roundUpToNextTenMinutes(nowMillis: Long): Long {
            val remainder = nowMillis % TEN_MINUTES_MILLIS
            return if (remainder == 0L) {
                nowMillis + TEN_MINUTES_MILLIS
            } else {
                nowMillis + (TEN_MINUTES_MILLIS - remainder)
            }
        }
    }
}
