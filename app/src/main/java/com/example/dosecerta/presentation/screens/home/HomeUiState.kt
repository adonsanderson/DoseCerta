package com.example.dosecerta.presentation.screens.home

import androidx.compose.runtime.Immutable
import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.presentation.common.UiState

@Immutable
data class HomeContent(
    val todayDoses: List<Dose>,
    val treatments: List<TreatmentWithDoses>,
    val todayAdherence: AdherenceSummary,
    val nowMillis: Long
)

@Immutable
data class HomeUiState(
    val contentState: UiState<HomeContent> = UiState.Loading,
    val feedbackMessage: String? = null
)
