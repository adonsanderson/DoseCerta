package com.example.dosecerta.domain.usecase

import app.cash.turbine.test
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.Treatment
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.TimeProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveTreatmentsUseCaseTest {

    private lateinit var treatmentRepository: TreatmentRepository
    private lateinit var useCase: ObserveTreatmentsUseCase

    private val nowMillis = 1_700_000_000_000L

    @Before
    fun setUp() {
        treatmentRepository = mockk()
        useCase = ObserveTreatmentsUseCase(
            treatmentRepository = treatmentRepository,
            timeProvider = TimeProvider { nowMillis }
        )
    }

    @Test
    fun `combines treatments and doses with adherence summary and reacts to updates`() = runTest {
        val treatment = Treatment(
            id = 1L,
            medicationName = "Amoxicilina",
            dosage = "500mg",
            startTimeMillis = nowMillis - 7_200_000L,
            intervalHours = 8,
            durationDays = 1
        )
        val initialDoses = listOf(
            Dose(
                id = 3L,
                treatmentId = 1L,
                sequenceNumber = 3,
                scheduledTimeMillis = nowMillis + 7_200_000L,
                status = DoseStatus.PENDING
            ),
            Dose(
                id = 1L,
                treatmentId = 1L,
                sequenceNumber = 1,
                scheduledTimeMillis = nowMillis - 7_200_000L,
                status = DoseStatus.TAKEN,
                takenAtMillis = nowMillis - 7_100_000L
            ),
            Dose(
                id = 2L,
                treatmentId = 1L,
                sequenceNumber = 2,
                scheduledTimeMillis = nowMillis - 60_000L,
                status = DoseStatus.PENDING
            )
        )

        val treatmentsFlow = MutableStateFlow(listOf(treatment))
        val dosesFlow = MutableStateFlow(initialDoses)

        every { treatmentRepository.observeAllTreatments() } returns treatmentsFlow
        every { treatmentRepository.observeAllDoses() } returns dosesFlow

        useCase().test {
            val firstEmission = awaitItem()
            assertEquals(1, firstEmission.size)
            val item = firstEmission.first()
            assertEquals(listOf(1L, 2L, 3L), item.doses.map { it.id })
            assertEquals(3, item.adherence.totalDoses)
            assertEquals(1, item.adherence.takenDoses)
            assertEquals(2, item.adherence.pendingDoses)
            assertEquals(1, item.adherence.overdueDoses)
            assertEquals(33, item.adherence.adherencePercentage)

            // Mark overdue dose as TAKEN and confirm reactive recalculation
            dosesFlow.value = initialDoses.map {
                if (it.id == 2L) it.copy(status = DoseStatus.TAKEN, takenAtMillis = nowMillis) else it
            }

            val secondEmission = awaitItem()
            val updated = secondEmission.first()
            assertEquals(2, updated.adherence.takenDoses)
            assertEquals(1, updated.adherence.pendingDoses)
            assertEquals(0, updated.adherence.overdueDoses)
            assertEquals(66, updated.adherence.adherencePercentage)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
