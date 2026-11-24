package com.example.dosecerta.presentation.screens.addtreatment

import androidx.compose.runtime.Immutable

@Immutable
data class AddTreatmentUiState(
    val medicationName: String = "",
    val dosage: String = "",
    val notes: String = "",
    val startTimeMillis: Long = 0L,
    val intervalHoursText: String = "8",
    val durationDaysText: String = "7",
    val medicationNameError: String? = null,
    val intervalError: String? = null,
    val durationError: String? = null,
    val previewDoseCount: Int? = 21,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedTreatmentId: Long? = null
) {
    companion object {
        val QUICK_INTERVAL_OPTIONS = listOf(4, 6, 8, 12, 24)
    }
}
