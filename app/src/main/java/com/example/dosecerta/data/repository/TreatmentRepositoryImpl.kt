package com.example.dosecerta.data.repository

import com.example.dosecerta.data.local.DatabaseTransactionRunner
import com.example.dosecerta.data.local.dao.DoseDao
import com.example.dosecerta.data.local.dao.TreatmentDao
import com.example.dosecerta.data.mapper.toDomain
import com.example.dosecerta.data.mapper.toEntity
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.Treatment
import com.example.dosecerta.domain.repository.TreatmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TreatmentRepositoryImpl(
    private val treatmentDao: TreatmentDao,
    private val doseDao: DoseDao,
    private val transactionRunner: DatabaseTransactionRunner
) : TreatmentRepository {

    override suspend fun saveTreatmentWithDoses(
        treatment: Treatment,
        doses: List<Dose>
    ): Pair<Long, List<Dose>> = transactionRunner.runInTransaction {
        val treatmentId = treatmentDao.insert(treatment.toEntity())
        val doseEntities = doses.map { it.toEntity(overrideTreatmentId = treatmentId) }
        val persistedIds = doseDao.insertAll(doseEntities)
        val persistedDoses = doses.mapIndexed { index, dose ->
            dose.copy(
                id = persistedIds.getOrElse(index) { dose.id },
                treatmentId = treatmentId
            )
        }
        treatmentId to persistedDoses
    }

    override fun observeAllTreatments(): Flow<List<Treatment>> =
        treatmentDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTreatmentById(treatmentId: Long): Flow<Treatment?> =
        treatmentDao.observeById(treatmentId).map { it?.toDomain() }

    override fun observeAllDoses(): Flow<List<Dose>> =
        doseDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeDosesByTreatmentId(treatmentId: Long): Flow<List<Dose>> =
        doseDao.observeByTreatmentId(treatmentId).map { entities -> entities.map { it.toDomain() } }

    override fun observeDosesInRange(startMillis: Long, endMillis: Long): Flow<List<Dose>> =
        doseDao.observeInRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getDoseById(doseId: Long): Dose? =
        doseDao.getById(doseId)?.toDomain()

    override suspend fun getDosesByTreatmentId(treatmentId: Long): List<Dose> =
        doseDao.getByTreatmentId(treatmentId).map { it.toDomain() }

    override suspend fun getFuturePendingDoses(nowMillis: Long): List<Dose> =
        doseDao.getFuturePending(DoseStatus.PENDING.name, nowMillis).map { it.toDomain() }

    override suspend fun updateDoseStatus(
        doseId: Long,
        status: DoseStatus,
        takenAtMillis: Long?
    ) {
        doseDao.updateStatus(
            doseId = doseId,
            status = status.name,
            takenAtMillis = takenAtMillis
        )
    }

    override suspend fun deleteTreatment(treatmentId: Long) {
        treatmentDao.deleteById(treatmentId)
    }
}
