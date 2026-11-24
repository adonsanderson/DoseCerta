package com.example.dosecerta.presentation.screens.home

import app.cash.turbine.test
import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.Treatment
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.domain.scheduler.TimeProvider
import com.example.dosecerta.domain.usecase.ObserveTodayDosesUseCase
import com.example.dosecerta.domain.usecase.ObserveTreatmentsUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeTreatmentsUseCase: ObserveTreatmentsUseCase = mockk()
    private val observeTodayDosesUseCase: ObserveTodayDosesUseCase = mockk()
    private val toggleDoseStatusUseCase: ToggleDoseStatusUseCase = mockk()
    private val nowMillis = 1_700_000_000_000L

    @Test
    fun `emits Loading then Empty when there are no treatments`() = runTest {
        every { observeTreatmentsUseCase() } returns flowOf(emptyList())
        every { observeTodayDosesUseCase(any(), any()) } returns flowOf(emptyList())

        val viewModel = HomeViewModel(
            observeTreatmentsUseCase = observeTreatmentsUseCase,
            observeTodayDosesUseCase = observeTodayDosesUseCase,
            toggleDoseStatusUseCase = toggleDoseStatusUseCase,
            timeProvider = TimeProvider { nowMillis }
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().contentState is UiState.Loading)
            assertTrue(awaitItem().contentState is UiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Loading then Success with treatments and today adherence`() = runTest {
        val dose1 = Dose(
            id = 1L,
            treatmentId = 10L,
            medicationName = "Amoxicilina",
            sequenceNumber = 1,
            scheduledTimeMillis = nowMillis - 3_600_000L,
            status = DoseStatus.TAKEN,
            takenAtMillis = nowMillis - 3_500_000L
        )
        val dose2 = Dose(
            id = 2L,
            treatmentId = 10L,
            medicationName = "Amoxicilina",
            sequenceNumber = 2,
            scheduledTimeMillis = nowMillis + 3_600_000L,
            status = DoseStatus.PENDING
        )
        val treatmentWithDoses = TreatmentWithDoses(
            treatment = Treatment(
                id = 10L,
                medicationName = "Amoxicilina",
                startTimeMillis = nowMillis - 3_600_000L,
                intervalHours = 8,
                durationDays = 5
            ),
            doses = listOf(dose1, dose2),
            adherence = AdherenceSummary(2, 1, 1, 0)
        )

        every { observeTreatmentsUseCase() } returns flowOf(listOf(treatmentWithDoses))
        every { observeTodayDosesUseCase(any(), any()) } returns flowOf(listOf(dose1, dose2))

        val viewModel = HomeViewModel(
            observeTreatmentsUseCase = observeTreatmentsUseCase,
            observeTodayDosesUseCase = observeTodayDosesUseCase,
            toggleDoseStatusUseCase = toggleDoseStatusUseCase,
            timeProvider = TimeProvider { nowMillis }
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().contentState is UiState.Loading)
            val success = awaitItem().contentState as UiState.Success
            assertEquals(1, success.data.treatments.size)
            assertEquals(2, success.data.todayDoses.size)
            assertEquals(50, success.data.todayAdherence.adherencePercentage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error when stream fails and surfaces feedback when toggle fails`() = runTest {
        every { observeTreatmentsUseCase() } returns flow {
            throw IllegalStateException("Falha no banco local")
        }
        every { observeTodayDosesUseCase(any(), any()) } returns flowOf(emptyList())
        coEvery { toggleDoseStatusUseCase(99L) } returns Result.failure(IllegalStateException("Erro ao marcar"))

        val viewModel = HomeViewModel(
            observeTreatmentsUseCase = observeTreatmentsUseCase,
            observeTodayDosesUseCase = observeTodayDosesUseCase,
            toggleDoseStatusUseCase = toggleDoseStatusUseCase,
            timeProvider = TimeProvider { nowMillis }
        )

        advanceUntilIdle()
        val errorState = viewModel.uiState.value.contentState
        assertTrue(errorState is UiState.Error)
        assertEquals("Falha no banco local", (errorState as UiState.Error).message)

        viewModel.onToggleDoseStatus(99L)
        advanceUntilIdle()
        assertEquals("Erro ao marcar", viewModel.uiState.value.feedbackMessage)
        coVerify(exactly = 1) { toggleDoseStatusUseCase(99L) }

        viewModel.onDismissFeedback()
        assertNull(viewModel.uiState.value.feedbackMessage)
    }
}
