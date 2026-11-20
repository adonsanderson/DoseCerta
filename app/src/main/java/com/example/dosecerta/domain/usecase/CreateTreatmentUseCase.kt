package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.Treatment
import com.example.dosecerta.domain.model.TreatmentValidationException
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import com.example.dosecerta.domain.scheduler.TimeProvider

class CreateTreatmentUseCase(
    private val calculateDoseScheduleUseCase: CalculateDoseScheduleUseCase,
    private val treatmentRepository: TreatmentRepository,
    private val doseAlarmScheduler: DoseAlarmScheduler,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(
        medicationName: String,
        dosage: String = "",
        notes: String = "",
        startTimeMillis: Long,
        intervalHours: Int,
        durationDays: Int
    ): Result<Long> = runCatching {
        val trimmedName = medicationName.trim()
        if (trimmedName.isEmpty()) {
            throw TreatmentValidationException("Informe o nome do medicamento.")
        }

        val trimmedDosage = dosage.trim()
        val trimmedNotes = notes.trim()
        val nowMillis = timeProvider.nowMillis()

        val calculatedDoses = calculateDoseScheduleUseCase(
            startTimeMillis = startTimeMillis,
            intervalHours = intervalHours,
            durationDays = durationDays,
            medicationName = trimmedName,
            dosage = trimmedDosage
        )

        val treatment = Treatment(
            medicationName = trimmedName,
            dosage = trimmedDosage,
            notes = trimmedNotes,
            startTimeMillis = startTimeMillis,
            intervalHours = intervalHours,
            durationDays = durationDays,
            createdAtMillis = nowMillis
        )

        val (treatmentId, persistedDoses) = treatmentRepository.saveTreatmentWithDoses(
            treatment = treatment,
            doses = calculatedDoses
        )

        val futurePendingDoses = persistedDoses.filter { it.isFuturePending(nowMillis) }
        if (futurePendingDoses.isNotEmpty()) {
            doseAlarmScheduler.scheduleDoses(futurePendingDoses)
        }

        treatmentId
    }
}
