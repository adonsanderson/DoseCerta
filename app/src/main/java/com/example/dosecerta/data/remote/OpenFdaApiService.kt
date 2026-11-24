package com.example.dosecerta.data.remote

import com.example.dosecerta.data.remote.dto.OpenFdaResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaApiService {

    @GET("drug/label.json")
    suspend fun searchDrugLabels(
        @Query("search") search: String,
        @Query("limit") limit: Int = DEFAULT_LIMIT
    ): OpenFdaResponseDto

    companion object {
        const val BASE_URL = "https://api.fda.gov/"
        const val DEFAULT_LIMIT = 10
    }
}
