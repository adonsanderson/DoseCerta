package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.repository.TreatmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveTodayDosesUseCase(
    private val treatmentRepository: TreatmentRepository
) {
    operator fun invoke(dayStartMillis: Long, dayEndMillis: Long): Flow<List<Dose>> =
        treatmentRepository.observeDosesInRange(dayStartMillis, dayEndMillis)
            .map { doses -> doses.sortedBy { it.scheduledTimeMillis } }
}
