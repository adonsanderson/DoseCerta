package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import com.example.dosecerta.domain.scheduler.TimeProvider

class ToggleDoseStatusUseCase(
    private val treatmentRepository: TreatmentRepository,
    private val doseAlarmScheduler: DoseAlarmScheduler,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(doseId: Long): Result<DoseStatus> = runCatching {
        val dose = treatmentRepository.getDoseById(doseId)
            ?: throw NoSuchElementException("Dose não encontrada (id=$doseId).")

        val nowMillis = timeProvider.nowMillis()

        if (dose.status == DoseStatus.PENDING) {
            treatmentRepository.updateDoseStatus(
                doseId = doseId,
                status = DoseStatus.TAKEN,
                takenAtMillis = nowMillis
            )
            doseAlarmScheduler.cancelDose(doseId)
            DoseStatus.TAKEN
        } else {
            treatmentRepository.updateDoseStatus(
                doseId = doseId,
                status = DoseStatus.PENDING,
                takenAtMillis = null
            )
            val revertedDose = dose.copy(status = DoseStatus.PENDING, takenAtMillis = null)
            if (revertedDose.isFuturePending(nowMillis)) {
                doseAlarmScheduler.scheduleDose(revertedDose)
            }
            DoseStatus.PENDING
        }
    }
}
