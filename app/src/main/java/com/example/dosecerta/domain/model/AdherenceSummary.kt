package com.example.dosecerta.domain.model

data class AdherenceSummary(
    val totalDoses: Int,
    val takenDoses: Int,
    val pendingDoses: Int,
    val overdueDoses: Int
) {
    val adherencePercentage: Int
        get() = if (totalDoses <= 0) 0 else ((takenDoses * 100.0) / totalDoses).toInt()

    val progressFraction: Float
        get() = if (totalDoses <= 0) 0f else takenDoses.toFloat() / totalDoses.toFloat()

    companion object {
        fun fromDoses(doses: List<Dose>, nowMillis: Long): AdherenceSummary {
            val total = doses.size
            val taken = doses.count { it.status == DoseStatus.TAKEN }
            val pending = total - taken
            val overdue = doses.count { it.isOverdue(nowMillis) }
            return AdherenceSummary(
                totalDoses = total,
                takenDoses = taken,
                pendingDoses = pending,
                overdueDoses = overdue
            )
        }
    }
}
