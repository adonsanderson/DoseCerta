package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.Treatment
import com.example.dosecerta.domain.model.TreatmentValidationException
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import com.example.dosecerta.domain.scheduler.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTreatmentUseCaseTest {

    private lateinit var treatmentRepository: TreatmentRepository
    private lateinit var doseAlarmScheduler: DoseAlarmScheduler
    private lateinit var useCase: CreateTreatmentUseCase

    private val nowMillis = 1_700_000_000_000L
    private val oneHourMillis = CalculateDoseScheduleUseCase.MILLIS_PER_HOUR

    @Before
    fun setUp() {
        treatmentRepository = mockk()
        doseAlarmScheduler = mockk(relaxed = true)
        useCase = CreateTreatmentUseCase(
            calculateDoseScheduleUseCase = CalculateDoseScheduleUseCase(),
            treatmentRepository = treatmentRepository,
            doseAlarmScheduler = doseAlarmScheduler,
            timeProvider = TimeProvider { nowMillis }
        )
    }

    @Test
    fun `creates treatment, trims fields, saves doses and schedules only future alarms`() = runTest {
        val startMillis = nowMillis - (2 * oneHourMillis) // 1st dose in past, 2nd & 3rd in future
        val treatmentSlot = slot<Treatment>()
        val dosesSlot = slot<List<Dose>>()

        coEvery {
            treatmentRepository.saveTreatmentWithDoses(capture(treatmentSlot), capture(dosesSlot))
        } answers {
            val savedDoses = dosesSlot.captured.mapIndexed { idx, dose ->
                dose.copy(id = (idx + 100L), treatmentId = 42L)
            }
            42L to savedDoses
        }

        val result = useCase(
            medicationName = "  Ibuprofeno  ",
            dosage = " 400mg ",
            notes = " Após almoço ",
            startTimeMillis = startMillis,
            intervalHours = 8,
            durationDays = 1
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
        assertEquals("Ibuprofeno", treatmentSlot.captured.medicationName)
        assertEquals("400mg", treatmentSlot.captured.dosage)
        assertEquals("Após almoço", treatmentSlot.captured.notes)
        assertEquals(nowMillis, treatmentSlot.captured.createdAtMillis)
        assertEquals(3, dosesSlot.captured.size)

        val scheduledSlot = slot<List<Dose>>()
        verify(exactly = 1) { doseAlarmScheduler.scheduleDoses(capture(scheduledSlot)) }
        assertEquals(2, scheduledSlot.captured.size)
        assertEquals(listOf(101L, 102L), scheduledSlot.captured.map { it.id })
    }

    @Test
    fun `does not schedule alarms when all calculated doses are in the past`() = runTest {
        val startMillis = nowMillis - (48 * oneHourMillis)

        coEvery { treatmentRepository.saveTreatmentWithDoses(any(), any()) } answers {
            val inputDoses = secondArg<List<Dose>>()
            7L to inputDoses.mapIndexed { idx, d -> d.copy(id = idx + 1L, treatmentId = 7L) }
        }

        val result = useCase(
            medicationName = "Dipirona",
            startTimeMillis = startMillis,
            intervalHours = 8,
            durationDays = 1
        )

        assertTrue(result.isSuccess)
        verify(exactly = 0) { doseAlarmScheduler.scheduleDoses(any()) }
    }

    @Test
    fun `returns failure and skips persistence when medication name is blank`() = runTest {
        val result = useCase(
            medicationName = "   ",
            startTimeMillis = nowMillis + oneHourMillis,
            intervalHours = 8,
            durationDays = 3
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TreatmentValidationException)
        coVerify(exactly = 0) { treatmentRepository.saveTreatmentWithDoses(any(), any()) }
        verify(exactly = 0) { doseAlarmScheduler.scheduleDoses(any()) }
    }

    @Test
    fun `returns failure and skips persistence when interval is invalid`() = runTest {
        val result = useCase(
            medicationName = "Paracetamol",
            startTimeMillis = nowMillis + oneHourMillis,
            intervalHours = 0,
            durationDays = 3
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TreatmentValidationException)
        coVerify(exactly = 0) { treatmentRepository.saveTreatmentWithDoses(any(), any()) }
        verify(exactly = 0) { doseAlarmScheduler.scheduleDoses(any()) }
    }
}
