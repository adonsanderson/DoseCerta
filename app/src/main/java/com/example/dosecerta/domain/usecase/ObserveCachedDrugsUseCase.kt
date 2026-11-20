package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.domain.repository.DrugInfoRepository
import kotlinx.coroutines.flow.Flow

class ObserveCachedDrugsUseCase(
    private val drugInfoRepository: DrugInfoRepository
) {
    operator fun invoke(): Flow<List<DrugInfo>> = drugInfoRepository.observeCachedDrugs()
}
