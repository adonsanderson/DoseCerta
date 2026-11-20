package com.example.dosecerta.domain.repository

import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.domain.model.DrugSearchResult
import kotlinx.coroutines.flow.Flow

interface DrugInfoRepository {
    suspend fun searchDrugInfo(query: String): Result<DrugSearchResult>
    fun observeCachedDrugs(): Flow<List<DrugInfo>>
    suspend fun getCachedDrugById(id: String): DrugInfo?
}
