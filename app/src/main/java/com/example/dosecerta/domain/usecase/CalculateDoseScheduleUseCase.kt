package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.TreatmentValidationException

/**
 * Pure Kotlin use case responsible for calculating the full chronological schedule of doses
 * for a medication treatment (RF02 / RN01).
 */
class CalculateDoseScheduleUseCase {

    operator fun invoke(
        startTimeMillis: Long,
        intervalHours: Int,
        durationDays: Int,
        treatmentId: Long = 0L,
        medicationName: String = "",
        dosage: String = ""
    ): List<Dose> {
        if (startTimeMillis < 0L) {
            throw TreatmentValidationException("A data/hora da primeira dose é inválida.")
        }
        if (intervalHours !in MIN_INTERVAL_HOURS..MAX_INTERVAL_HOURS) {
            throw TreatmentValidationException(
                "O intervalo entre doses deve estar entre $MIN_INTERVAL_HOURS e $MAX_INTERVAL_HOURS horas."
            )
        }
        if (durationDays !in MIN_DURATION_DAYS..MAX_DURATION_DAYS) {
            throw TreatmentValidationException(
                "A duração do tratamento deve estar entre $MIN_DURATION_DAYS e $MAX_DURATION_DAYS dias."
            )
        }

        val stepMillis = intervalHours.toLong() * MILLIS_PER_HOUR
        val totalDurationMillis = durationDays.toLong() * HOURS_PER_DAY * MILLIS_PER_HOUR
        val endTimeMillis = startTimeMillis + totalDurationMillis

        val doses = mutableListOf<Dose>()
        var currentTimeMillis = startTimeMillis
        var sequenceNumber = 1

        while (currentTimeMillis < endTimeMillis) {
            doses.add(
                Dose(
                    treatmentId = treatmentId,
                    medicationName = medicationName,
                    dosage = dosage,
                    sequenceNumber = sequenceNumber,
                    scheduledTimeMillis = currentTimeMillis,
                    status = DoseStatus.PENDING,
                    takenAtMillis = null
                )
            )
            currentTimeMillis += stepMillis
            sequenceNumber++
        }

        return doses
    }

    companion object {
        const val MILLIS_PER_HOUR: Long = 3_600_000L
        const val HOURS_PER_DAY: Long = 24L
        const val MIN_INTERVAL_HOURS: Int = 1
        const val MAX_INTERVAL_HOURS: Int = 168
        const val MIN_DURATION_DAYS: Int = 1
        const val MAX_DURATION_DAYS: Int = 365
    }
}
