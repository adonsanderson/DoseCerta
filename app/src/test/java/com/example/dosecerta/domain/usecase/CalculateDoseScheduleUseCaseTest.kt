package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.TreatmentValidationException
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateDoseScheduleUseCaseTest {

    private lateinit var useCase: CalculateDoseScheduleUseCase

    @Before
    fun setUp() {
        useCase = CalculateDoseScheduleUseCase()
    }

    @Test
    fun `8h interval for 1 day generates 3 exact doses crossing midnight`() {
        val startMillis = Instant.parse("2026-11-20T08:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 8,
            durationDays = 1,
            treatmentId = 10L,
            medicationName = "Amoxicilina",
            dosage = "500mg"
        )

        assertEquals(3, result.size)
        assertEquals(
            listOf(
                Instant.parse("2026-11-20T08:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-20T16:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-21T00:00:00Z").toEpochMilli()
            ),
            result.map { it.scheduledTimeMillis }
        )
        assertEquals(listOf(1, 2, 3), result.map { it.sequenceNumber })
        assertTrue(result.all { it.status == DoseStatus.PENDING })
        assertTrue(result.all { it.takenAtMillis == null })
        assertTrue(result.all { it.treatmentId == 10L })
        assertTrue(result.all { it.medicationName == "Amoxicilina" && it.dosage == "500mg" })
    }

    @Test
    fun `12h interval for 5 days generates 10 chronologically ordered doses`() {
        val startMillis = Instant.parse("2026-11-20T07:30:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 12,
            durationDays = 5
        )

        assertEquals(10, result.size)
        assertEquals(1, result.first().sequenceNumber)
        assertEquals(10, result.last().sequenceNumber)
        assertEquals(
            Instant.parse("2026-11-24T19:30:00Z").toEpochMilli(),
            result.last().scheduledTimeMillis
        )
        assertTrue(result.zipWithNext().all { (a, b) -> a.scheduledTimeMillis < b.scheduledTimeMillis })
    }

    @Test
    fun `6h interval for 7 days generates 28 doses`() {
        val startMillis = Instant.parse("2026-11-20T06:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 6,
            durationDays = 7
        )

        assertEquals(28, result.size)
        assertEquals(
            Instant.parse("2026-11-27T00:00:00Z").toEpochMilli(),
            result.last().scheduledTimeMillis
        )
    }

    @Test
    fun `4h interval for 1 day generates 6 doses`() {
        val startMillis = Instant.parse("2026-11-20T00:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 4,
            durationDays = 1
        )

        assertEquals(6, result.size)
        assertEquals(
            Instant.parse("2026-11-20T20:00:00Z").toEpochMilli(),
            result.last().scheduledTimeMillis
        )
    }

    @Test
    fun `24h interval for 3 days generates 3 doses`() {
        val startMillis = Instant.parse("2026-11-20T09:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 24,
            durationDays = 3
        )

        assertEquals(3, result.size)
        assertEquals(
            listOf(
                Instant.parse("2026-11-20T09:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-21T09:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-22T09:00:00Z").toEpochMilli()
            ),
            result.map { it.scheduledTimeMillis }
        )
    }

    @Test
    fun `non-divisor interval 10h for 1 day generates 3 doses within 24h window`() {
        val startMillis = Instant.parse("2026-11-20T00:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 10,
            durationDays = 1
        )

        assertEquals(3, result.size)
        assertEquals(
            listOf(
                Instant.parse("2026-11-20T00:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-20T10:00:00Z").toEpochMilli(),
                Instant.parse("2026-11-20T20:00:00Z").toEpochMilli()
            ),
            result.map { it.scheduledTimeMillis }
        )
    }

    @Test
    fun `RN01 interval greater than total duration still generates initial dose`() {
        val startMillis = Instant.parse("2026-11-20T10:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 48,
            durationDays = 1
        )

        assertEquals(1, result.size)
        assertEquals(startMillis, result.first().scheduledTimeMillis)
        assertEquals(1, result.first().sequenceNumber)
        assertNull(result.first().takenAtMillis)
    }

    @Test
    fun `handles leap year february rollover accurately`() {
        val startMillis = Instant.parse("2024-02-28T20:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 8,
            durationDays = 2
        )

        assertEquals(6, result.size)
        assertEquals(
            listOf(
                Instant.parse("2024-02-28T20:00:00Z").toEpochMilli(),
                Instant.parse("2024-02-29T04:00:00Z").toEpochMilli(),
                Instant.parse("2024-02-29T12:00:00Z").toEpochMilli(),
                Instant.parse("2024-02-29T20:00:00Z").toEpochMilli(),
                Instant.parse("2024-03-01T04:00:00Z").toEpochMilli(),
                Instant.parse("2024-03-01T12:00:00Z").toEpochMilli()
            ),
            result.map { it.scheduledTimeMillis }
        )
    }

    @Test
    fun `handles year rollover from December 31 to January 1 accurately`() {
        val startMillis = Instant.parse("2026-12-31T20:00:00Z").toEpochMilli()

        val result = useCase(
            startTimeMillis = startMillis,
            intervalHours = 6,
            durationDays = 1
        )

        assertEquals(4, result.size)
        assertEquals(
            listOf(
                Instant.parse("2026-12-31T20:00:00Z").toEpochMilli(),
                Instant.parse("2027-01-01T02:00:00Z").toEpochMilli(),
                Instant.parse("2027-01-01T08:00:00Z").toEpochMilli(),
                Instant.parse("2027-01-01T14:00:00Z").toEpochMilli()
            ),
            result.map { it.scheduledTimeMillis }
        )
    }

    @Test(expected = TreatmentValidationException::class)
    fun `throws validation exception when intervalHours is zero`() {
        useCase(startTimeMillis = 1_000_000L, intervalHours = 0, durationDays = 3)
    }

    @Test(expected = TreatmentValidationException::class)
    fun `throws validation exception when intervalHours is negative`() {
        useCase(startTimeMillis = 1_000_000L, intervalHours = -8, durationDays = 3)
    }

    @Test(expected = TreatmentValidationException::class)
    fun `throws validation exception when intervalHours exceeds maximum`() {
        useCase(startTimeMillis = 1_000_000L, intervalHours = 169, durationDays = 3)
    }

    @Test(expected = TreatmentValidationException::class)
    fun `throws validation exception when durationDays is zero`() {
        useCase(startTimeMillis = 1_000_000L, intervalHours = 8, durationDays = 0)
    }

    @Test(expected = TreatmentValidationException::class)
    fun `throws validation exception when durationDays exceeds maximum`() {
        useCase(startTimeMillis = 1_000_000L, intervalHours = 8, durationDays = 366)
    }

    @Test(expected = TreatmentValidationException::class)
    fun `throws validation exception when startTimeMillis is negative`() {
        useCase(startTimeMillis = -1L, intervalHours = 8, durationDays = 3)
    }
}
