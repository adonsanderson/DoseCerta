package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveTreatmentsUseCase(
    private val treatmentRepository: TreatmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<List<TreatmentWithDoses>> =
        combine(
            treatmentRepository.observeAllTreatments(),
            treatmentRepository.observeAllDoses()
        ) { treatments, allDoses ->
            val nowMillis = timeProvider.nowMillis()
            val dosesByTreatment = allDoses.groupBy { it.treatmentId }
            treatments.map { treatment ->
                val treatmentDoses = dosesByTreatment[treatment.id]
                    .orEmpty()
                    .sortedBy { it.scheduledTimeMillis }
                TreatmentWithDoses(
                    treatment = treatment,
                    doses = treatmentDoses,
                    adherence = AdherenceSummary.fromDoses(treatmentDoses, nowMillis)
                )
            }
        }
}
