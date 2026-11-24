package com.example.dosecerta.data.mapper

import com.example.dosecerta.data.local.entity.DrugInfoEntity
import com.example.dosecerta.data.remote.dto.DrugLabelDto
import com.example.dosecerta.domain.model.DrugInfo

private val WHITESPACE_REGEX = Regex("\\s+")
private val NON_ID_CHARS_REGEX = Regex("[^a-z0-9_]+")

fun DrugInfoEntity.toDomain(): DrugInfo = DrugInfo(
    id = id,
    brandName = brandName,
    genericName = genericName,
    manufacturer = manufacturer,
    indicationsAndUsage = indicationsAndUsage,
    warnings = warnings,
    dosageAndAdministration = dosageAndAdministration,
    contraindications = contraindications,
    adverseReactions = adverseReactions,
    searchQuery = searchQuery,
    cachedAtMillis = cachedAtMillis
)

fun DrugInfo.toEntity(): DrugInfoEntity = DrugInfoEntity(
    id = id,
    brandName = brandName,
    genericName = genericName,
    manufacturer = manufacturer,
    indicationsAndUsage = indicationsAndUsage,
    warnings = warnings,
    dosageAndAdministration = dosageAndAdministration,
    contraindications = contraindications,
    adverseReactions = adverseReactions,
    searchQuery = searchQuery,
    cachedAtMillis = cachedAtMillis
)

fun DrugLabelDto.toEntity(
    searchQuery: String,
    cachedAtMillis: Long
): DrugInfoEntity {
    val normalizedQuery = searchQuery.trim()
    val brand = openfda?.brandName
        ?.firstOrNull { it.isNotBlank() }
        ?.cleanText()
        ?: normalizedQuery.replaceFirstChar { it.uppercase() }

    val generic = openfda?.genericName
        ?.firstOrNull { it.isNotBlank() }
        ?.cleanText()
        ?: brand

    val manufacturer = openfda?.manufacturerName
        ?.firstOrNull { it.isNotBlank() }
        ?.cleanText()
        ?: "Não informado"

    val warningsText = warnings.joinSections().ifBlank {
        warningsAndCautions.joinSections()
    }

    val resolvedId = id?.takeIf { it.isNotBlank() }
        ?: setId?.takeIf { it.isNotBlank() }
        ?: "${brand}_${generic}_${manufacturer}"
            .lowercase()
            .replace(NON_ID_CHARS_REGEX, "_")
            .trim('_')

    return DrugInfoEntity(
        id = resolvedId,
        brandName = brand,
        genericName = generic,
        manufacturer = manufacturer,
        indicationsAndUsage = indicationsAndUsage.joinSections(),
        warnings = warningsText,
        dosageAndAdministration = dosageAndAdministration.joinSections(),
        contraindications = contraindications.joinSections(),
        adverseReactions = adverseReactions.joinSections(),
        searchQuery = normalizedQuery.lowercase(),
        cachedAtMillis = cachedAtMillis
    )
}

private fun List<String>?.joinSections(): String =
    this.orEmpty()
        .map { it.cleanText() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n\n")

private fun String.cleanText(): String =
    trim().replace(WHITESPACE_REGEX, " ")
