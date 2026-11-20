package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveTreatmentDetailUseCase(
    private val treatmentRepository: TreatmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(treatmentId: Long): Flow<TreatmentWithDoses?> =
        combine(
            treatmentRepository.observeTreatmentById(treatmentId),
            treatmentRepository.observeDosesByTreatmentId(treatmentId)
        ) { treatment, doses ->
            if (treatment == null) {
                null
            } else {
                val sortedDoses = doses.sortedBy { it.scheduledTimeMillis }
                TreatmentWithDoses(
                    treatment = treatment,
                    doses = sortedDoses,
                    adherence = AdherenceSummary.fromDoses(sortedDoses, timeProvider.nowMillis())
                )
            }
        }
}
