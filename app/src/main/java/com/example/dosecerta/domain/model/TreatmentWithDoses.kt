package com.example.dosecerta.domain.model

data class TreatmentWithDoses(
    val treatment: Treatment,
    val doses: List<Dose>,
    val adherence: AdherenceSummary
)
