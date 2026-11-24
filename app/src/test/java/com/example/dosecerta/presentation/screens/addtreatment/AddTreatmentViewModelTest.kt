package com.example.dosecerta.presentation.screens.addtreatment

import com.example.dosecerta.domain.scheduler.TimeProvider
import com.example.dosecerta.domain.usecase.CalculateDoseScheduleUseCase
import com.example.dosecerta.domain.usecase.CreateTreatmentUseCase
import com.example.dosecerta.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTreatmentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val createTreatmentUseCase: CreateTreatmentUseCase = mockk()
    private val calculateUseCase = CalculateDoseScheduleUseCase()
    private val nowMillis = 1_700_000_000_000L

    private fun createViewModel() = AddTreatmentViewModel(
        calculateDoseScheduleUseCase = calculateUseCase,
        createTreatmentUseCase = createTreatmentUseCase,
        timeProvider = TimeProvider { nowMillis }
    )

    @Test
    fun `updates live dose preview count when interval and duration change`() {
        val viewModel = createViewModel()

        // Default: 8h for 7 days = 21 doses
        assertEquals(21, viewModel.uiState.value.previewDoseCount)

        viewModel.onIntervalHoursChange("6")
        viewModel.onDurationDaysChange("3")
        assertEquals(12, viewModel.uiState.value.previewDoseCount)

        viewModel.onIntervalHoursChange("0")
        assertNull(viewModel.uiState.value.previewDoseCount)
    }

    @Test
    fun `shows inline validation errors and skips save when fields are invalid`() = runTest {
        val viewModel = createViewModel()

        viewModel.onMedicationNameChange("   ")
        viewModel.onIntervalHoursChange("0")
        viewModel.onDurationDaysChange("0")
        viewModel.onSaveTreatment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.medicationNameError)
        assertNotNull(state.intervalError)
        assertNotNull(state.durationError)
        assertNull(state.savedTreatmentId)
        coVerify(exactly = 0) {
            createTreatmentUseCase(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `saves valid treatment and exposes savedTreatmentId`() = runTest {
        coEvery {
            createTreatmentUseCase(
                medicationName = "Dipirona",
                dosage = "500mg",
                notes = "Se febre",
                startTimeMillis = any(),
                intervalHours = 6,
                durationDays = 2
            )
        } returns Result.success(77L)

        val viewModel = createViewModel()
        viewModel.onMedicationNameChange("Dipirona")
        viewModel.onDosageChange("500mg")
        viewModel.onNotesChange("Se febre")
        viewModel.onIntervalHoursChange("6")
        viewModel.onDurationDaysChange("2")

        viewModel.onSaveTreatment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals(77L, state.savedTreatmentId)
        assertNull(state.errorMessage)
    }

    @Test
    fun `surfaces errorMessage when persistence fails`() = runTest {
        coEvery {
            createTreatmentUseCase(any(), any(), any(), any(), any(), any())
        } returns Result.failure(IllegalStateException("Erro ao gravar no banco"))

        val viewModel = createViewModel()
        viewModel.onMedicationNameChange("Ibuprofeno")
        viewModel.onSaveTreatment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertNull(state.savedTreatmentId)
        assertEquals("Erro ao gravar no banco", state.errorMessage)
    }
}
