package com.example.dosecerta.domain.model

data class Treatment(
    val id: Long = 0L,
    val medicationName: String,
    val dosage: String = "",
    val notes: String = "",
    val startTimeMillis: Long,
    val intervalHours: Int,
    val durationDays: Int,
    val createdAtMillis: Long = 0L
)
