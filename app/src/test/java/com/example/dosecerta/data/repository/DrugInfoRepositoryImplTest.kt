package com.example.dosecerta.data.repository

import app.cash.turbine.test
import com.example.dosecerta.data.local.dao.DrugInfoDao
import com.example.dosecerta.data.local.entity.DrugInfoEntity
import com.example.dosecerta.data.remote.OpenFdaApiService
import com.example.dosecerta.data.remote.dto.DrugLabelDto
import com.example.dosecerta.data.remote.dto.OpenFdaResponseDto
import com.example.dosecerta.data.remote.dto.OpenFdaSectionDto
import com.example.dosecerta.domain.scheduler.TimeProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DrugInfoRepositoryImplTest {

    private lateinit var apiService: OpenFdaApiService
    private lateinit var drugInfoDao: DrugInfoDao
    private lateinit var repository: DrugInfoRepositoryImpl

    private val nowMillis = 1_700_500_000_000L

    @Before
    fun setUp() {
        apiService = mockk()
        drugInfoDao = mockk()
        repository = DrugInfoRepositoryImpl(
            openFdaApiService = apiService,
            drugInfoDao = drugInfoDao,
            timeProvider = TimeProvider { nowMillis }
        )
    }

    @Test
    fun `remote success maps DTOs, upserts into Room cache, and returns fresh results`() = runTest {
        val searchSlot = slot<String>()
        val cachedEntitiesSlot = slot<List<DrugInfoEntity>>()

        coEvery { apiService.searchDrugLabels(search = capture(searchSlot), limit = any()) } returns
            OpenFdaResponseDto(
                results = listOf(
                    DrugLabelDto(
                        id = "fda-ibuprofen-1",
                        openfda = OpenFdaSectionDto(
                            brandName = listOf("Advil  Liqui-Gels"),
                            genericName = listOf("IBUPROFEN"),
                            manufacturerName = listOf("Haleon US")
                        ),
                        indicationsAndUsage = listOf("Temporarily relieves minor aches and pains."),
                        warnings = null,
                        warningsAndCautions = listOf("Do not exceed recommended dose."),
                        dosageAndAdministration = listOf("Take 1 capsule every 4 to 6 hours."),
                        contraindications = listOf("Allergy to NSAIDs."),
                        adverseReactions = listOf("Stomach upset.")
                    )
                )
            )
        coEvery { drugInfoDao.upsertAll(capture(cachedEntitiesSlot)) } just runs

        val result = repository.searchDrugInfo("  \"ibuprofen\"+ ")

        assertTrue(result.isSuccess)
        assertEquals(
            "openfda.brand_name:\"ibuprofen\" OR openfda.generic_name:\"ibuprofen\"",
            searchSlot.captured
        )
        val payload = result.getOrThrow()
        assertFalse(payload.isFromCache)
        assertEquals(1, payload.items.size)
        val item = payload.items.first()
        assertEquals("fda-ibuprofen-1", item.id)
        assertEquals("Advil Liqui-Gels", item.brandName)
        assertEquals("IBUPROFEN", item.genericName)
        assertEquals("Do not exceed recommended dose.", item.warnings)
        assertEquals(nowMillis, item.cachedAtMillis)

        coVerify(exactly = 1) { drugInfoDao.upsertAll(any()) }
        assertEquals("ibuprofen", cachedEntitiesSlot.captured.first().searchQuery)
    }

    @Test
    fun `offline IOException falls back to cached Room results with isFromCache true`() = runTest {
        coEvery { apiService.searchDrugLabels(any(), any()) } throws IOException("No network")
        coEvery { drugInfoDao.searchCached("ibuprofen") } returns listOf(
            sampleCachedEntity(id = "cached-1", brand = "Advil", generic = "Ibuprofen")
        )

        val result = repository.searchDrugInfo("ibuprofen")

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertTrue(payload.isFromCache)
        assertEquals(1, payload.items.size)
        assertEquals("cached-1", payload.items.first().id)
    }

    @Test
    fun `offline IOException with empty cache returns failure with friendly message`() = runTest {
        coEvery { apiService.searchDrugLabels(any(), any()) } throws IOException("Timeout")
        coEvery { drugInfoDao.searchCached("amoxicillin") } returns emptyList()

        val result = repository.searchDrugInfo("amoxicillin")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Sem conexão"))
    }

    @Test
    fun `HTTP 404 with empty cache returns empty success instead of error`() = runTest {
        val errorBody = """{"error":{"code":"NOT_FOUND"}}""".toResponseBody("application/json".toMediaType())
        val http404 = HttpException(Response.error<OpenFdaResponseDto>(404, errorBody))
        coEvery { apiService.searchDrugLabels(any(), any()) } throws http404
        coEvery { drugInfoDao.searchCached("medicamento_inexistente") } returns emptyList()

        val result = repository.searchDrugInfo("medicamento_inexistente")

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertTrue(payload.items.isEmpty())
        assertFalse(payload.isFromCache)
    }

    @Test
    fun `HTTP 404 falls back to cache when matching cached entry exists`() = runTest {
        val errorBody = """{"error":{"code":"NOT_FOUND"}}""".toResponseBody("application/json".toMediaType())
        val http404 = HttpException(Response.error<OpenFdaResponseDto>(404, errorBody))
        coEvery { apiService.searchDrugLabels(any(), any()) } throws http404
        coEvery { drugInfoDao.searchCached("advil") } returns listOf(
            sampleCachedEntity(id = "cached-advil", brand = "Advil", generic = "Ibuprofen")
        )

        val result = repository.searchDrugInfo("advil")

        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertTrue(payload.isFromCache)
        assertEquals(1, payload.items.size)
    }

    @Test
    fun `HTTP 500 without cache returns failure`() = runTest {
        val errorBody = "Server error".toResponseBody("text/plain".toMediaType())
        val http500 = HttpException(Response.error<OpenFdaResponseDto>(500, errorBody))
        coEvery { apiService.searchDrugLabels(any(), any()) } throws http500
        coEvery { drugInfoDao.searchCached("aspirin") } returns emptyList()

        val result = repository.searchDrugInfo("aspirin")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("500"))
    }

    @Test
    fun `observeCachedDrugs emits reactive updates from Room DAO`() = runTest {
        val cachedFlow = MutableStateFlow(
            listOf(sampleCachedEntity(id = "1", brand = "Tylenol", generic = "Acetaminophen"))
        )
        every { drugInfoDao.observeAllCached() } returns cachedFlow

        repository.observeCachedDrugs().test {
            val first = awaitItem()
            assertEquals(listOf("Tylenol"), first.map { it.brandName })

            cachedFlow.value = cachedFlow.value +
                sampleCachedEntity(id = "2", brand = "Advil", generic = "Ibuprofen")

            val second = awaitItem()
            assertEquals(listOf("Tylenol", "Advil"), second.map { it.brandName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleCachedEntity(
        id: String,
        brand: String,
        generic: String
    ): DrugInfoEntity = DrugInfoEntity(
        id = id,
        brandName = brand,
        genericName = generic,
        manufacturer = "Lab",
        indicationsAndUsage = "Pain relief",
        warnings = "Read label",
        dosageAndAdministration = "1 tablet",
        contraindications = "None",
        adverseReactions = "Mild",
        searchQuery = generic.lowercase(),
        cachedAtMillis = nowMillis - 10_000L
    )
}
