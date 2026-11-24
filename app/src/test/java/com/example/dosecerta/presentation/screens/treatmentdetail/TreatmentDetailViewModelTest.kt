package com.example.dosecerta.presentation.screens.treatmentdetail

import app.cash.turbine.test
import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.Treatment
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.domain.scheduler.TimeProvider
import com.example.dosecerta.domain.usecase.DeleteTreatmentUseCase
import com.example.dosecerta.domain.usecase.ObserveTreatmentDetailUseCase
import com.example.dosecerta.domain.usecase.ToggleDoseStatusUseCase
import com.example.dosecerta.presentation.MainDispatcherRule
import com.example.dosecerta.presentation.common.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TreatmentDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeDetailUseCase: ObserveTreatmentDetailUseCase = mockk()
    private val toggleDoseUseCase: ToggleDoseStatusUseCase = mockk()
    private val deleteTreatmentUseCase: DeleteTreatmentUseCase = mockk()
    private val nowMillis = 1_700_000_000_000L

    @Test
    fun `emits Loading then Success with treatment timeline and supports deletion`() = runTest {
        val detail = TreatmentWithDoses(
            treatment = Treatment(
                id = 5L,
                medicationName = "Cefalexina",
                dosage = "500mg",
                startTimeMillis = nowMillis,
                intervalHours = 6,
                durationDays = 5
            ),
            doses = listOf(
                Dose(
                    id = 10L,
                    treatmentId = 5L,
                    medicationName = "Cefalexina",
                    sequenceNumber = 1,
                    scheduledTimeMillis = nowMillis,
                    status = DoseStatus.PENDING
                )
            ),
            adherence = AdherenceSummary(1, 0, 1, 0)
        )
        every { observeDetailUseCase(5L) } returns flowOf(detail)
        coEvery { toggleDoseUseCase(10L) } returns Result.success(DoseStatus.TAKEN)
        coEvery { deleteTreatmentUseCase(5L) } returns Result.success(Unit)

        val viewModel = TreatmentDetailViewModel(
            treatmentId = 5L,
            observeTreatmentDetailUseCase = observeDetailUseCase,
            toggleDoseStatusUseCase = toggleDoseUseCase,
            deleteTreatmentUseCase = deleteTreatmentUseCase,
            timeProvider = TimeProvider { nowMillis }
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().contentState is UiState.Loading)
            val success = awaitItem().contentState as UiState.Success
            assertEquals("Cefalexina", success.data.treatment.medicationName)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.onToggleDoseStatus(10L)
        viewModel.onDeleteTreatment()
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleDoseUseCase(10L) }
        coVerify(exactly = 1) { deleteTreatmentUseCase(5L) }
        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun `emits Empty when treatment is not found`() = runTest {
        every { observeDetailUseCase(404L) } returns flowOf(null)

        val viewModel = TreatmentDetailViewModel(
            treatmentId = 404L,
            observeTreatmentDetailUseCase = observeDetailUseCase,
            toggleDoseStatusUseCase = toggleDoseUseCase,
            deleteTreatmentUseCase = deleteTreatmentUseCase,
            timeProvider = TimeProvider { nowMillis }
        )

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.contentState is UiState.Empty)
    }

    @Test
    fun `emits Error when observation stream fails`() = runTest {
        every { observeDetailUseCase(9L) } returns flow {
            throw RuntimeException("Erro inesperado")
        }

        val viewModel = TreatmentDetailViewModel(
            treatmentId = 9L,
            observeTreatmentDetailUseCase = observeDetailUseCase,
            toggleDoseStatusUseCase = toggleDoseUseCase,
            deleteTreatmentUseCase = deleteTreatmentUseCase,
            timeProvider = TimeProvider { nowMillis }
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value.contentState
        assertTrue(state is UiState.Error)
        assertEquals("Erro inesperado", (state as UiState.Error).message)
    }
}
