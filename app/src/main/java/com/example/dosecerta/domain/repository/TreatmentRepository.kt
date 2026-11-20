package com.example.dosecerta.domain.repository

import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.Treatment
import kotlinx.coroutines.flow.Flow

interface TreatmentRepository {
    suspend fun saveTreatmentWithDoses(treatment: Treatment, doses: List<Dose>): Pair<Long, List<Dose>>
    fun observeAllTreatments(): Flow<List<Treatment>>
    fun observeTreatmentById(treatmentId: Long): Flow<Treatment?>
    fun observeAllDoses(): Flow<List<Dose>>
    fun observeDosesByTreatmentId(treatmentId: Long): Flow<List<Dose>>
    fun observeDosesInRange(startMillis: Long, endMillis: Long): Flow<List<Dose>>
    suspend fun getDoseById(doseId: Long): Dose?
    suspend fun getDosesByTreatmentId(treatmentId: Long): List<Dose>
    suspend fun getFuturePendingDoses(nowMillis: Long): List<Dose>
    suspend fun updateDoseStatus(doseId: Long, status: DoseStatus, takenAtMillis: Long?)
    suspend fun deleteTreatment(treatmentId: Long)
}
