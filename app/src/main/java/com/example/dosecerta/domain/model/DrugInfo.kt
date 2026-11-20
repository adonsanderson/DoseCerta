package com.example.dosecerta.domain.model

data class DrugInfo(
    val id: String,
    val brandName: String,
    val genericName: String,
    val manufacturer: String,
    val indicationsAndUsage: String,
    val warnings: String,
    val dosageAndAdministration: String,
    val contraindications: String,
    val adverseReactions: String,
    val searchQuery: String,
    val cachedAtMillis: Long
)
