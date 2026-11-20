package com.example.dosecerta.domain.model

data class Dose(
    val id: Long = 0L,
    val treatmentId: Long,
    val medicationName: String = "",
    val dosage: String = "",
    val sequenceNumber: Int,
    val scheduledTimeMillis: Long,
    val status: DoseStatus = DoseStatus.PENDING,
    val takenAtMillis: Long? = null
) {
    fun isOverdue(nowMillis: Long): Boolean =
        status == DoseStatus.PENDING && scheduledTimeMillis < nowMillis

    fun isFuturePending(nowMillis: Long): Boolean =
        status == DoseStatus.PENDING && scheduledTimeMillis > nowMillis
}
