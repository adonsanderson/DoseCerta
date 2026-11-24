package com.example.dosecerta.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "doses",
    foreignKeys = [
        ForeignKey(
            entity = TreatmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["treatmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["treatmentId"]),
        Index(value = ["scheduledTimeMillis"])
    ]
)
data class DoseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val treatmentId: Long,
    val medicationName: String,
    val dosage: String,
    val sequenceNumber: Int,
    val scheduledTimeMillis: Long,
    val status: String,
    val takenAtMillis: Long? = null
)
