package com.example.dosecerta.presentation.screens.drugsearch

import app.cash.turbine.test
import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.domain.model.DrugSearchResult
import com.example.dosecerta.domain.usecase.ObserveCachedDrugsUseCase
import com.example.dosecerta.domain.usecase.SearchDrugInfoUseCase
import com.example.dosecerta.presentation.MainDispatcherRule
import com.example.dosecerta.presentation.common.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrugSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchDrugInfoUseCase: SearchDrugInfoUseCase = mockk()
    private val observeCachedDrugsUseCase: ObserveCachedDrugsUseCase = mockk()

    private fun sampleDrug(id: String, brand: String) = DrugInfo(
        id = id,
        brandName = brand,
        genericName = "IBUPROFEN",
        manufacturer = "Lab",
        indicationsAndUsage = "Pain relief",
        warnings = "Do not exceed dose",
        dosageAndAdministration = "1 tablet every 6h",
        contraindications = "Allergy",
        adverseReactions = "Stomach upset",
        searchQuery = "ibuprofen",
        cachedAtMillis = 1_700_000_000_000L
    )

    @Test
    fun `displays cached offline history when query is empty and Empty when cache is empty`() = runTest {
        val cachedFlow = MutableStateFlow<List<DrugInfo>>(emptyList())
        every { observeCachedDrugsUseCase() } returns cachedFlow

        val viewModel = DrugSearchViewModel(searchDrugInfoUseCase, observeCachedDrugsUseCase)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.contentState is UiState.Empty)

        val cachedDrug = sampleDrug("1", "Advil")
        cachedFlow.value = listOf(cachedDrug)
        advanceUntilIdle()

        val success = viewModel.uiState.value.contentState as UiState.Success
        assertTrue(success.data.isFromCache)
        assertTrue(success.data.isShowingRecentHistory)
        assertEquals(listOf(cachedDrug), success.data.items)
    }

    @Test
    fun `search transitions Loading to Success with fresh remote OpenFDA data`() = runTest {
        val cachedFlow = MutableStateFlow<List<DrugInfo>>(emptyList())
        val freshDrug = sampleDrug("fda-1", "Advil")
        every { observeCachedDrugsUseCase() } returns cachedFlow
        coEvery { searchDrugInfoUseCase("Ibuprofen") } returns Result.success(
            DrugSearchResult(items = listOf(freshDrug), isFromCache = false)
        )

        val viewModel = DrugSearchViewModel(searchDrugInfoUseCase, observeCachedDrugsUseCase)
        advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // initial Empty state

            viewModel.onQueryChange("Ibuprofen")
            awaitItem() // query updated

            viewModel.onSearch()
            assertTrue(awaitItem().contentState is UiState.Loading)

            val success = awaitItem().contentState as UiState.Success
            assertFalse(success.data.isFromCache)
            assertFalse(success.data.isShowingRecentHistory)
            assertEquals("Advil", success.data.items.first().brandName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search emits Empty when OpenFDA returns no matches and Error on network failure`() = runTest {
        every { observeCachedDrugsUseCase() } returns MutableStateFlow(emptyList())
        coEvery { searchDrugInfoUseCase("unknownDrug") } returns Result.success(
            DrugSearchResult(items = emptyList(), isFromCache = false)
        )
        coEvery { searchDrugInfoUseCase("offlineDrug") } returns Result.failure(
            IllegalStateException("Sem conexão com a internet")
        )

        val viewModel = DrugSearchViewModel(searchDrugInfoUseCase, observeCachedDrugsUseCase)
        advanceUntilIdle()

        viewModel.onQueryChange("unknownDrug")
        viewModel.onSearch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.contentState is UiState.Empty)

        viewModel.onQueryChange("offlineDrug")
        viewModel.onSearch()
        advanceUntilIdle()
        val errorState = viewModel.uiState.value.contentState as UiState.Error
        assertEquals("Sem conexão com a internet", errorState.message)
        assertTrue(errorState.canRetry)
    }

    @Test
    fun `RN06 short query does not trigger search and clearQuery restores offline history`() = runTest {
        val cachedDrug = sampleDrug("cached-1", "Motrin")
        every { observeCachedDrugsUseCase() } returns MutableStateFlow(listOf(cachedDrug))

        val viewModel = DrugSearchViewModel(searchDrugInfoUseCase, observeCachedDrugsUseCase)
        advanceUntilIdle()

        viewModel.onQueryChange("a")
        viewModel.onSearch()
        advanceUntilIdle()

        coVerify(exactly = 0) { searchDrugInfoUseCase(any()) }

        viewModel.onSelectDrug(cachedDrug)
        assertEquals(cachedDrug, viewModel.uiState.value.selectedDrug)

        viewModel.onDismissDetail()
        assertNull(viewModel.uiState.value.selectedDrug)

        viewModel.onClearQuery()
        assertEquals("", viewModel.uiState.value.query)
        val restored = viewModel.uiState.value.contentState as UiState.Success
        assertTrue(restored.data.isShowingRecentHistory)
    }
}
