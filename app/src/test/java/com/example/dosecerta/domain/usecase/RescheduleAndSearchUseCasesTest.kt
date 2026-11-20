package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.DrugSearchResult
import com.example.dosecerta.domain.model.TreatmentValidationException
import com.example.dosecerta.domain.repository.DrugInfoRepository
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import com.example.dosecerta.domain.scheduler.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleAndSearchUseCasesTest {

    @Test
    fun `reschedulePendingAlarms schedules only future pending doses and returns count`() = runTest {
        val now = 1_700_000_000_000L
        val repository = mockk<TreatmentRepository>()
        val scheduler = mockk<DoseAlarmScheduler>(relaxed = true)
        val futureDose = Dose(
            id = 1L,
            treatmentId = 1L,
            sequenceNumber = 1,
            scheduledTimeMillis = now + 60_000L,
            status = DoseStatus.PENDING
        )
        val pastDose = Dose(
            id = 2L,
            treatmentId = 1L,
            sequenceNumber = 2,
            scheduledTimeMillis = now - 60_000L,
            status = DoseStatus.PENDING
        )
        coEvery { repository.getFuturePendingDoses(now) } returns listOf(futureDose, pastDose)

        val useCase = ReschedulePendingAlarmsUseCase(repository, scheduler, TimeProvider { now })
        val count = useCase()

        assertEquals(1, count)
        verify(exactly = 1) { scheduler.scheduleDoses(listOf(futureDose)) }
    }

    @Test
    fun `reschedulePendingAlarms skips scheduler when there are no future doses`() = runTest {
        val now = 1_700_000_000_000L
        val repository = mockk<TreatmentRepository>()
        val scheduler = mockk<DoseAlarmScheduler>(relaxed = true)
        coEvery { repository.getFuturePendingDoses(now) } returns emptyList()

        val useCase = ReschedulePendingAlarmsUseCase(repository, scheduler, TimeProvider { now })
        val count = useCase()

        assertEquals(0, count)
        verify(exactly = 0) { scheduler.scheduleDoses(any()) }
    }

    @Test
    fun `RN06 searchDrugInfo rejects query shorter than 2 chars without hitting repository`() = runTest {
        val repository = mockk<DrugInfoRepository>()
        val useCase = SearchDrugInfoUseCase(repository)

        val emptyResult = useCase("   ")
        val oneCharResult = useCase(" a ")

        assertTrue(emptyResult.isFailure)
        assertTrue(emptyResult.exceptionOrNull() is TreatmentValidationException)
        assertTrue(oneCharResult.isFailure)
        assertTrue(oneCharResult.exceptionOrNull() is TreatmentValidationException)
        coVerify(exactly = 0) { repository.searchDrugInfo(any()) }
    }

    @Test
    fun `searchDrugInfo trims query and delegates to repository`() = runTest {
        val repository = mockk<DrugInfoRepository>()
        val expected = Result.success(DrugSearchResult(items = emptyList(), isFromCache = false))
        coEvery { repository.searchDrugInfo("ibuprofen") } returns expected

        val useCase = SearchDrugInfoUseCase(repository)
        val result = useCase("  ibuprofen  ")

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.searchDrugInfo("ibuprofen") }
    }
}
