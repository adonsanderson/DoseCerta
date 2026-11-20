package com.example.dosecerta.domain.model

data class DrugSearchResult(
    val items: List<DrugInfo>,
    val isFromCache: Boolean
)
