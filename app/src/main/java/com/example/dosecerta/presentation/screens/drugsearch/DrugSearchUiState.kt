package com.example.dosecerta.presentation.screens.drugsearch

import androidx.compose.runtime.Immutable
import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.presentation.common.UiState

@Immutable
data class DrugSearchContent(
    val items: List<DrugInfo>,
    val isFromCache: Boolean,
    val isShowingRecentHistory: Boolean
)

@Immutable
data class DrugSearchUiState(
    val query: String = "",
    val contentState: UiState<DrugSearchContent> = UiState.Loading,
    val selectedDrug: DrugInfo? = null
)
