package com.example.dosecerta.data.repository

import app.cash.turbine.test
import com.example.dosecerta.data.local.DatabaseTransactionRunner
import com.example.dosecerta.data.local.dao.DoseDao
import com.example.dosecerta.data.local.dao.TreatmentDao
import com.example.dosecerta.data.local.entity.DoseEntity
import com.example.dosecerta.data.local.entity.TreatmentEntity
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.Treatment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TreatmentRepositoryImplTest {

    private lateinit var treatmentDao: TreatmentDao
    private lateinit var doseDao: DoseDao
    private lateinit var repository: TreatmentRepositoryImpl

    private var transactionInvoked = false

    @Before
    fun setUp() {
        treatmentDao = mockk()
        doseDao = mockk()
        transactionInvoked = false
        val transactionRunner = object : DatabaseTransactionRunner {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T {
                transactionInvoked = true
                return block()
            }
        }
        repository = TreatmentRepositoryImpl(
            treatmentDao = treatmentDao,
            doseDao = doseDao,
            transactionRunner = transactionRunner
        )
    }

    @Test
    fun `saveTreatmentWithDoses runs in transaction and assigns generated IDs to doses`() = runTest {
        val treatment = Treatment(
            medicationName = "Amoxicilina",
            dosage = "500mg",
            notes = "Após café",
            startTimeMillis = 1_700_000_000_000L,
            intervalHours = 8,
            durationDays = 1,
            createdAtMillis = 1_699_999_000_000L
        )
        val doses = listOf(
            Dose(
                treatmentId = 0L,
                medicationName = "Amoxicilina",
                dosage = "500mg",
                sequenceNumber = 1,
                scheduledTimeMillis = 1_700_000_000_000L
            ),
            Dose(
                treatmentId = 0L,
                medicationName = "Amoxicilina",
                dosage = "500mg",
                sequenceNumber = 2,
                scheduledTimeMillis = 1_700_028_800_000L
            )
        )
        val capturedDoseEntities = slot<List<DoseEntity>>()
        coEvery { treatmentDao.insert(any()) } returns 99L
        coEvery { doseDao.insertAll(capture(capturedDoseEntities)) } returns listOf(501L, 502L)

        val (savedTreatmentId, persistedDoses) = repository.saveTreatmentWithDoses(treatment, doses)

        assertTrue(transactionInvoked)
        assertEquals(99L, savedTreatmentId)
        assertEquals(listOf(99L, 99L), capturedDoseEntities.captured.map { it.treatmentId })
        assertEquals(listOf(501L, 502L), persistedDoses.map { it.id })
        assertEquals(listOf(99L, 99L), persistedDoses.map { it.treatmentId })
    }

    @Test
    fun `observeAllTreatments emits mapped domain treatments reactively`() = runTest {
        val entityFlow = MutableStateFlow(
            listOf(
                TreatmentEntity(
                    id = 1L,
                    medicationName = "Ibuprofeno",
                    dosage = "400mg",
                    notes = "",
                    startTimeMillis = 1000L,
                    intervalHours = 8,
                    durationDays = 3,
                    createdAtMillis = 900L
                )
            )
        )
        every { treatmentDao.observeAll() } returns entityFlow

        repository.observeAllTreatments().test {
            val first = awaitItem()
            assertEquals(1, first.size)
            assertEquals("Ibuprofeno", first.first().medicationName)

            entityFlow.value = entityFlow.value + TreatmentEntity(
                id = 2L,
                medicationName = "Dipirona",
                dosage = "1g",
                notes = "Se dor",
                startTimeMillis = 2000L,
                intervalHours = 6,
                durationDays = 2,
                createdAtMillis = 1900L
            )

            val second = awaitItem()
            assertEquals(listOf("Ibuprofeno", "Dipirona"), second.map { it.medicationName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeDosesInRange maps entities and falls back safely on unknown status`() = runTest {
        val doseFlow = MutableStateFlow(
            listOf(
                DoseEntity(
                    id = 10L,
                    treatmentId = 1L,
                    medicationName = "Dipirona",
                    dosage = "500mg",
                    sequenceNumber = 1,
                    scheduledTimeMillis = 5000L,
                    status = "TAKEN",
                    takenAtMillis = 5010L
                ),
                DoseEntity(
                    id = 11L,
                    treatmentId = 1L,
                    medicationName = "Dipirona",
                    dosage = "500mg",
                    sequenceNumber = 2,
                    scheduledTimeMillis = 9000L,
                    status = "LEGACY_UNKNOWN",
                    takenAtMillis = null
                )
            )
        )
        every { doseDao.observeInRange(0L, 10_000L) } returns doseFlow

        repository.observeDosesInRange(0L, 10_000L).test {
            val emitted = awaitItem()
            assertEquals(DoseStatus.TAKEN, emitted[0].status)
            assertEquals(DoseStatus.PENDING, emitted[1].status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFuturePendingDoses queries with PENDING status and maps results`() = runTest {
        coEvery { doseDao.getFuturePending("PENDING", 10_000L) } returns listOf(
            DoseEntity(
                id = 7L,
                treatmentId = 2L,
                medicationName = "Losartana",
                dosage = "50mg",
                sequenceNumber = 1,
                scheduledTimeMillis = 20_000L,
                status = "PENDING",
                takenAtMillis = null
            )
        )

        val result = repository.getFuturePendingDoses(10_000L)

        assertEquals(1, result.size)
        assertEquals(7L, result.first().id)
        assertEquals(DoseStatus.PENDING, result.first().status)
    }

    @Test
    fun `updateDoseStatus and deleteTreatment delegate cleanly to DAOs`() = runTest {
        coEvery { doseDao.updateStatus(5L, "TAKEN", 12345L) } just runs
        coEvery { treatmentDao.deleteById(3L) } just runs

        repository.updateDoseStatus(5L, DoseStatus.TAKEN, 12345L)
        repository.deleteTreatment(3L)

        coVerify(exactly = 1) { doseDao.updateStatus(5L, "TAKEN", 12345L) }
        coVerify(exactly = 1) { treatmentDao.deleteById(3L) }
    }
}
