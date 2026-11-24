package com.example.dosecerta.data.mapper

import com.example.dosecerta.data.local.entity.TreatmentEntity
import com.example.dosecerta.domain.model.Treatment

fun TreatmentEntity.toDomain(): Treatment = Treatment(
    id = id,
    medicationName = medicationName,
    dosage = dosage,
    notes = notes,
    startTimeMillis = startTimeMillis,
    intervalHours = intervalHours,
    durationDays = durationDays,
    createdAtMillis = createdAtMillis
)

fun Treatment.toEntity(): TreatmentEntity = TreatmentEntity(
    id = id,
    medicationName = medicationName,
    dosage = dosage,
    notes = notes,
    startTimeMillis = startTimeMillis,
    intervalHours = intervalHours,
    durationDays = durationDays,
    createdAtMillis = createdAtMillis
)
