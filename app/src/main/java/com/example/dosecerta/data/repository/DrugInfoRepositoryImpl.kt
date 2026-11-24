package com.example.dosecerta.data.repository

import com.example.dosecerta.data.local.dao.DrugInfoDao
import com.example.dosecerta.data.mapper.toDomain
import com.example.dosecerta.data.mapper.toEntity
import com.example.dosecerta.data.remote.OpenFdaApiService
import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.domain.model.DrugSearchResult
import com.example.dosecerta.domain.repository.DrugInfoRepository
import com.example.dosecerta.domain.scheduler.TimeProvider
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class DrugInfoRepositoryImpl(
    private val openFdaApiService: OpenFdaApiService,
    private val drugInfoDao: DrugInfoDao,
    private val timeProvider: TimeProvider
) : DrugInfoRepository {

    override suspend fun searchDrugInfo(query: String): Result<DrugSearchResult> {
        val sanitizedQuery = sanitizeQuery(query)
        if (sanitizedQuery.isBlank()) {
            return Result.success(DrugSearchResult(items = emptyList(), isFromCache = false))
        }

        val searchExpression = buildSearchExpression(sanitizedQuery)

        return try {
            val response = openFdaApiService.searchDrugLabels(search = searchExpression)
            val nowMillis = timeProvider.nowMillis()
            val entities = response.results
                .orEmpty()
                .map { dto -> dto.toEntity(searchQuery = sanitizedQuery, cachedAtMillis = nowMillis) }
                .distinctBy { it.id }

            if (entities.isNotEmpty()) {
                drugInfoDao.upsertAll(entities)
                Result.success(
                    DrugSearchResult(
                        items = entities.map { it.toDomain() },
                        isFromCache = false
                    )
                )
            } else {
                fallbackOrEmpty(sanitizedQuery)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (httpException: HttpException) {
            if (httpException.code() == HTTP_NOT_FOUND) {
                fallbackOrEmpty(sanitizedQuery)
            } else {
                fallbackOrError(
                    query = sanitizedQuery,
                    errorMessage = "Não foi possível consultar a base OpenFDA (código ${httpException.code()})."
                )
            }
        } catch (_: IOException) {
            fallbackOrError(
                query = sanitizedQuery,
                errorMessage = "Sem conexão com a internet e nenhum registro em cache para \"$sanitizedQuery\"."
            )
        } catch (_: Exception) {
            fallbackOrError(
                query = sanitizedQuery,
                errorMessage = "Ocorreu um erro inesperado ao consultar as informações do medicamento."
            )
        }
    }

    override fun observeCachedDrugs(): Flow<List<DrugInfo>> =
        drugInfoDao.observeAllCached().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getCachedDrugById(id: String): DrugInfo? =
        drugInfoDao.getById(id)?.toDomain()

    private suspend fun fallbackOrEmpty(query: String): Result<DrugSearchResult> {
        val cached = drugInfoDao.searchCached(query).map { it.toDomain() }
        return if (cached.isNotEmpty()) {
            Result.success(DrugSearchResult(items = cached, isFromCache = true))
        } else {
            Result.success(DrugSearchResult(items = emptyList(), isFromCache = false))
        }
    }

    private suspend fun fallbackOrError(
        query: String,
        errorMessage: String
    ): Result<DrugSearchResult> {
        val cached = drugInfoDao.searchCached(query).map { it.toDomain() }
        return if (cached.isNotEmpty()) {
            Result.success(DrugSearchResult(items = cached, isFromCache = true))
        } else {
            Result.failure(IllegalStateException(errorMessage))
        }
    }

    private fun sanitizeQuery(rawQuery: String): String =
        rawQuery
            .replace(LUCENE_SPECIAL_CHARS_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()

    private fun buildSearchExpression(sanitizedQuery: String): String =
        "openfda.brand_name:\"$sanitizedQuery\" OR openfda.generic_name:\"$sanitizedQuery\""

    companion object {
        private const val HTTP_NOT_FOUND = 404
        private val LUCENE_SPECIAL_CHARS_REGEX = Regex("[\"\\\\+\\-!(){}\\[\\]^~*?:;/]")
        private val MULTI_SPACE_REGEX = Regex("\\s+")
    }
}
