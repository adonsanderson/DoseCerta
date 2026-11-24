package com.example.dosecerta.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drug_info_cache")
data class DrugInfoEntity(
    @PrimaryKey
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
