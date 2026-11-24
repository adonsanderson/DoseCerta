package com.example.dosecerta.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenFdaSectionDto(
    @Json(name = "brand_name")
    val brandName: List<String>? = null,
    @Json(name = "generic_name")
    val genericName: List<String>? = null,
    @Json(name = "manufacturer_name")
    val manufacturerName: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class DrugLabelDto(
    @Json(name = "id")
    val id: String? = null,
    @Json(name = "set_id")
    val setId: String? = null,
    @Json(name = "openfda")
    val openfda: OpenFdaSectionDto? = null,
    @Json(name = "indications_and_usage")
    val indicationsAndUsage: List<String>? = null,
    @Json(name = "warnings")
    val warnings: List<String>? = null,
    @Json(name = "warnings_and_cautions")
    val warningsAndCautions: List<String>? = null,
    @Json(name = "dosage_and_administration")
    val dosageAndAdministration: List<String>? = null,
    @Json(name = "contraindications")
    val contraindications: List<String>? = null,
    @Json(name = "adverse_reactions")
    val adverseReactions: List<String>? = null
)
