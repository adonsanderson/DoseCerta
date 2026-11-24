package com.example.dosecerta.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenFdaResponseDto(
    @Json(name = "meta")
    val meta: OpenFdaMetaDto? = null,
    @Json(name = "results")
    val results: List<DrugLabelDto>? = null
)
