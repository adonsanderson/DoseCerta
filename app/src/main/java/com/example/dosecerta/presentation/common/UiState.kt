package com.example.dosecerta.presentation.common

import androidx.compose.runtime.Immutable

@Immutable
sealed interface UiState<out T> {
    @Immutable
    data object Loading : UiState<Nothing>

    @Immutable
    data class Success<T>(val data: T) : UiState<T>

    @Immutable
    data class Empty(val message: String) : UiState<Nothing>

    @Immutable
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : UiState<Nothing>
}
