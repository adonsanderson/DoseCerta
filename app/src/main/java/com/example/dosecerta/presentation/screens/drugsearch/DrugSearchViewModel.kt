package com.example.dosecerta.presentation.screens.drugsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.domain.usecase.ObserveCachedDrugsUseCase
import com.example.dosecerta.domain.usecase.SearchDrugInfoUseCase
import com.example.dosecerta.presentation.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DrugSearchViewModel(
    private val searchDrugInfoUseCase: SearchDrugInfoUseCase,
    private val observeCachedDrugsUseCase: ObserveCachedDrugsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrugSearchUiState(contentState = UiState.Loading))
    val uiState: StateFlow<DrugSearchUiState> = _uiState.asStateFlow()

    private var cachedDrugs: List<DrugInfo> = emptyList()
    private var isShowingSearchResults: Boolean = false
    private var searchJob: Job? = null

    init {
        observeOfflineHistory()
    }

    private fun observeOfflineHistory() {
        viewModelScope.launch {
            observeCachedDrugsUseCase()
                .catch { error ->
                    if (!isShowingSearchResults) {
                        _uiState.update {
                            it.copy(
                                contentState = UiState.Error(
                                    message = error.message
                                        ?: "Não foi possível carregar o histórico offline.",
                                    canRetry = false
                                )
                            )
                        }
                    }
                }
                .collect { items ->
                    cachedDrugs = items
                    if (!isShowingSearchResults) {
                        emitCachedHistoryState()
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        if (newQuery.trim().length < SearchDrugInfoUseCase.MIN_QUERY_LENGTH) {
            searchJob?.cancel()
            isShowingSearchResults = false
            emitCachedHistoryState()
        }
    }

    fun onClearQuery() {
        searchJob?.cancel()
        isShowingSearchResults = false
        _uiState.update { it.copy(query = "") }
        emitCachedHistoryState()
    }

    fun onSearch() {
        val currentQuery = _uiState.value.query.trim()
        if (currentQuery.length < SearchDrugInfoUseCase.MIN_QUERY_LENGTH) {
            isShowingSearchResults = false
            emitCachedHistoryState()
            return
        }

        searchJob?.cancel()
        isShowingSearchResults = true
        _uiState.update { it.copy(contentState = UiState.Loading) }

        searchJob = viewModelScope.launch {
            searchDrugInfoUseCase(currentQuery)
                .onSuccess { result ->
                    val nextState = if (result.items.isEmpty()) {
                        UiState.Empty(
                            "Nenhuma bula encontrada para \"$currentQuery\". Tente o princípio ativo em inglês (ex.: Ibuprofen)."
                        )
                    } else {
                        UiState.Success(
                            DrugSearchContent(
                                items = result.items,
                                isFromCache = result.isFromCache,
                                isShowingRecentHistory = false
                            )
                        )
                    }
                    _uiState.update { it.copy(contentState = nextState) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            contentState = UiState.Error(
                                message = error.message
                                    ?: "Não foi possível consultar a base OpenFDA.",
                                canRetry = true
                            )
                        )
                    }
                }
        }
    }

    fun onSelectDrug(drug: DrugInfo) {
        _uiState.update { it.copy(selectedDrug = drug) }
    }

    fun onDismissDetail() {
        _uiState.update { it.copy(selectedDrug = null) }
    }

    private fun emitCachedHistoryState() {
        val state = if (cachedDrugs.isEmpty()) {
            UiState.Empty("Nenhuma bula consultada ainda.")
        } else {
            UiState.Success(
                DrugSearchContent(
                    items = cachedDrugs,
                    isFromCache = true,
                    isShowingRecentHistory = true
                )
            )
        }
        _uiState.update { it.copy(contentState = state) }
    }
}
