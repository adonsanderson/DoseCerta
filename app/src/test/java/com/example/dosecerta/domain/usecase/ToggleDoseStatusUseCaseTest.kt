package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import com.example.dosecerta.domain.scheduler.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleDoseStatusUseCaseTest {

    private lateinit var treatmentRepository: TreatmentRepository
    private lateinit var doseAlarmScheduler: DoseAlarmScheduler
    private lateinit var useCase: ToggleDoseStatusUseCase

    private val nowMillis = 1_700_000_000_000L

    @Before
    fun setUp() {
        treatmentRepository = mockk()
        doseAlarmScheduler = mockk(relaxed = true)
        useCase = ToggleDoseStatusUseCase(
            treatmentRepository = treatmentRepository,
            doseAlarmScheduler = doseAlarmScheduler,
            timeProvider = TimeProvider { nowMillis }
        )
    }

    @Test
    fun `marking pending dose as taken updates timestamp and cancels alarm`() = runTest {
        val pendingDose = Dose(
            id = 15L,
            treatmentId = 1L,
            medicationName = "Amoxicilina",
            sequenceNumber = 1,
            scheduledTimeMillis = nowMillis + 3_600_000L,
            status = DoseStatus.PENDING
        )
        coEvery { treatmentRepository.getDoseById(15L) } returns pendingDose
        coEvery { treatmentRepository.updateDoseStatus(15L, DoseStatus.TAKEN, nowMillis) } just runs

        val result = useCase(15L)

        assertTrue(result.isSuccess)
        assertEquals(DoseStatus.TAKEN, result.getOrNull())
        coVerify(exactly = 1) {
            treatmentRepository.updateDoseStatus(15L, DoseStatus.TAKEN, nowMillis)
        }
        verify(exactly = 1) { doseAlarmScheduler.cancelDose(15L) }
        verify(exactly = 0) { doseAlarmScheduler.scheduleDose(any()) }
    }

    @Test
    fun `reverting future taken dose to pending clears timestamp and reschedules alarm`() = runTest {
        val futureTakenDose = Dose(
            id = 20L,
            treatmentId = 1L,
            medicationName = "Amoxicilina",
            sequenceNumber = 2,
            scheduledTimeMillis = nowMillis + 7_200_000L,
            status = DoseStatus.TAKEN,
            takenAtMillis = nowMillis - 10_000L
        )
        coEvery { treatmentRepository.getDoseById(20L) } returns futureTakenDose
        coEvery { treatmentRepository.updateDoseStatus(20L, DoseStatus.PENDING, null) } just runs

        val result = useCase(20L)

        assertTrue(result.isSuccess)
        assertEquals(DoseStatus.PENDING, result.getOrNull())
        coVerify(exactly = 1) {
            treatmentRepository.updateDoseStatus(20L, DoseStatus.PENDING, null)
        }
        verify(exactly = 1) {
            doseAlarmScheduler.scheduleDose(
                futureTakenDose.copy(status = DoseStatus.PENDING, takenAtMillis = null)
            )
        }
    }

    @Test
    fun `reverting past taken dose to pending does not reschedule alarm`() = runTest {
        val pastTakenDose = Dose(
            id = 21L,
            treatmentId = 1L,
            medicationName = "Dipirona",
            sequenceNumber = 1,
            scheduledTimeMillis = nowMillis - 3_600_000L,
            status = DoseStatus.TAKEN,
            takenAtMillis = nowMillis - 3_500_000L
        )
        coEvery { treatmentRepository.getDoseById(21L) } returns pastTakenDose
        coEvery { treatmentRepository.updateDoseStatus(21L, DoseStatus.PENDING, null) } just runs

        val result = useCase(21L)

        assertTrue(result.isSuccess)
        assertEquals(DoseStatus.PENDING, result.getOrNull())
        verify(exactly = 0) { doseAlarmScheduler.scheduleDose(any()) }
    }

    @Test
    fun `returns failure when dose does not exist`() = runTest {
        coEvery { treatmentRepository.getDoseById(999L) } returns null

        val result = useCase(999L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
        coVerify(exactly = 0) { treatmentRepository.updateDoseStatus(any(), any(), any()) }
        verify(exactly = 0) { doseAlarmScheduler.cancelDose(any()) }
        verify(exactly = 0) { doseAlarmScheduler.scheduleDose(any()) }
    }
}
