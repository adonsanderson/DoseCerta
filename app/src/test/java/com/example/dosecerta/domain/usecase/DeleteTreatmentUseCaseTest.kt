package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteTreatmentUseCaseTest {

    private lateinit var treatmentRepository: TreatmentRepository
    private lateinit var doseAlarmScheduler: DoseAlarmScheduler
    private lateinit var useCase: DeleteTreatmentUseCase

    @Before
    fun setUp() {
        treatmentRepository = mockk()
        doseAlarmScheduler = mockk(relaxed = true)
        useCase = DeleteTreatmentUseCase(treatmentRepository, doseAlarmScheduler)
    }

    @Test
    fun `cancels all associated dose alarms before deleting treatment`() = runTest {
        val doses = listOf(
            Dose(id = 10L, treatmentId = 5L, sequenceNumber = 1, scheduledTimeMillis = 1000L),
            Dose(id = 11L, treatmentId = 5L, sequenceNumber = 2, scheduledTimeMillis = 2000L)
        )
        coEvery { treatmentRepository.getDosesByTreatmentId(5L) } returns doses
        coEvery { treatmentRepository.deleteTreatment(5L) } just runs

        val result = useCase(5L)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { doseAlarmScheduler.cancelDoses(listOf(10L, 11L)) }
        coVerify(exactly = 1) { treatmentRepository.deleteTreatment(5L) }
    }

    @Test
    fun `skips alarm cancellation when treatment has no doses and still deletes treatment`() = runTest {
        coEvery { treatmentRepository.getDosesByTreatmentId(8L) } returns emptyList()
        coEvery { treatmentRepository.deleteTreatment(8L) } just runs

        val result = useCase(8L)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { doseAlarmScheduler.cancelDoses(any()) }
        coVerify(exactly = 1) { treatmentRepository.deleteTreatment(8L) }
    }
}
