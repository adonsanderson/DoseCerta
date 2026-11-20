package com.example.dosecerta.domain.usecase

import com.example.dosecerta.domain.model.DrugSearchResult
import com.example.dosecerta.domain.model.TreatmentValidationException
import com.example.dosecerta.domain.repository.DrugInfoRepository

class SearchDrugInfoUseCase(
    private val drugInfoRepository: DrugInfoRepository
) {
    suspend operator fun invoke(query: String): Result<DrugSearchResult> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < MIN_QUERY_LENGTH) {
            return Result.failure(
                TreatmentValidationException(
                    "Digite pelo menos $MIN_QUERY_LENGTH caracteres para buscar medicamentos."
                )
            )
        }
        return drugInfoRepository.searchDrugInfo(trimmedQuery)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
