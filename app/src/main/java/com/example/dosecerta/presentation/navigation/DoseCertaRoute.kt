package com.example.dosecerta.presentation.navigation

object DoseCertaRoute {
    const val HOME = "home"
    const val ADD_TREATMENT = "add_treatment"
    const val DRUG_SEARCH = "drug_search"

    const val ARG_TREATMENT_ID = "treatmentId"
    const val TREATMENT_DETAIL_PATTERN = "treatment_detail/{$ARG_TREATMENT_ID}"

    fun treatmentDetail(treatmentId: Long): String = "treatment_detail/$treatmentId"
}
