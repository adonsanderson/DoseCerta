package com.example.dosecerta.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenFdaMetaDto(
    @Json(name = "disclaimer")
    val disclaimer: String? = null,
    @Json(name = "last_updated")
    val lastUpdated: String? = null
)
