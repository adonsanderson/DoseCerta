package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.repository.TreatmentRepository
import com.example.dosecerta.domain.scheduler.DoseAlarmScheduler
import com.example.dosecerta.domain.scheduler.TimeProvider

class ReschedulePendingAlarmsUseCase(
    private val treatmentRepository: TreatmentRepository,
    private val doseAlarmScheduler: DoseAlarmScheduler,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(): Int {
        val nowMillis = timeProvider.nowMillis()
        val pendingFutureDoses = treatmentRepository
            .getFuturePendingDoses(nowMillis)
            .filter { it.isFuturePending(nowMillis) }

        if (pendingFutureDoses.isNotEmpty()) {
            doseAlarmScheduler.scheduleDoses(pendingFutureDoses)
        }
        return pendingFutureDoses.size
    }
}
