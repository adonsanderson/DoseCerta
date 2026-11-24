package com.example.dosecerta.presentation.screens.treatmentdetail

import androidx.compose.runtime.Immutable
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.presentation.common.UiState

@Immutable
data class TreatmentDetailUiState(
    val contentState: UiState<TreatmentWithDoses> = UiState.Loading,
    val nowMillis: Long = 0L,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val feedbackMessage: String? = null
)
