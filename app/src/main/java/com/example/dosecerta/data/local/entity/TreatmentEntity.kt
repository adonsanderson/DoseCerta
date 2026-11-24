package com.example.dosecerta.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treatments")
data class TreatmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val medicationName: String,
    val dosage: String,
    val notes: String,
    val startTimeMillis: Long,
    val intervalHours: Int,
    val durationDays: Int,
    val createdAtMillis: Long
)
