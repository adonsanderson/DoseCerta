package com.example.dosecerta.data.mapper

import com.example.dosecerta.data.local.entity.DoseEntity
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus

fun DoseEntity.toDomain(): Dose = Dose(
    id = id,
    treatmentId = treatmentId,
    medicationName = medicationName,
    dosage = dosage,
    sequenceNumber = sequenceNumber,
    scheduledTimeMillis = scheduledTimeMillis,
    status = DoseStatus.entries.firstOrNull { it.name == status } ?: DoseStatus.PENDING,
    takenAtMillis = takenAtMillis
)

fun Dose.toEntity(overrideTreatmentId: Long = treatmentId): DoseEntity = DoseEntity(
    id = id,
    treatmentId = overrideTreatmentId,
    medicationName = medicationName,
    dosage = dosage,
    sequenceNumber = sequenceNumber,
    scheduledTimeMillis = scheduledTimeMillis,
    status = status.name,
    takenAtMillis = takenAtMillis
)
