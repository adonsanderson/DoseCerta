package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler

class DeleteTreatmentUseCase(
    private val treatmentRepository: TreatmentRepository,
    private val doseAlarmScheduler: DoseAlarmScheduler
) {
    suspend operator fun invoke(treatmentId: Long): Result<Unit> = runCatching {
        val doseIds = treatmentRepository.getDosesByTreatmentId(treatmentId).map { it.id }
        if (doseIds.isNotEmpty()) {
            doseAlarmScheduler.cancelDoses(doseIds)
        }
        treatmentRepository.deleteTreatment(treatmentId)
    }
}
