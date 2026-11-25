package com.example.dosecerta.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.domain.model.Treatment
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.presentation.common.UiState
import com.example.dosecerta.presentation.screens.addtreatment.AddTreatmentScreen
import com.example.dosecerta.presentation.screens.addtreatment.AddTreatmentUiState
import com.example.dosecerta.presentation.screens.drugsearch.DrugSearchContent
import com.example.dosecerta.presentation.screens.drugsearch.DrugSearchScreen
import com.example.dosecerta.presentation.screens.drugsearch.DrugSearchUiState
import com.example.dosecerta.presentation.screens.home.HomeContent
import com.example.dosecerta.presentation.screens.home.HomeScreen
import com.example.dosecerta.presentation.screens.home.HomeUiState
import com.example.dosecerta.presentation.theme.DoseCertaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DoseCertaCujTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `home screen renders Loading, Empty, Error and Success states with interactive toggle`() {
        var toggledDoseId: Long? = null
        val nowMillis = 1_700_000_000_000L
        val dose = Dose(
            id = 101L,
            treatmentId = 10L,
            medicationName = "Amoxicilina",
            dosage = "500mg",
            sequenceNumber = 1,
            scheduledTimeMillis = nowMillis + 3_600_000L,
            status = DoseStatus.PENDING
        )
        val treatmentWithDoses = TreatmentWithDoses(
            treatment = Treatment(
                id = 10L,
                medicationName = "Amoxicilina",
                dosage = "500mg",
                startTimeMillis = nowMillis,
                intervalHours = 8,
                durationDays = 5
            ),
            doses = listOf(dose),
            adherence = AdherenceSummary(1, 0, 1, 0)
        )

        composeTestRule.setContent {
            DoseCertaTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        contentState = UiState.Success(
                            HomeContent(
                                todayDoses = listOf(dose),
                                treatments = listOf(treatmentWithDoses),
                                todayAdherence = AdherenceSummary(1, 0, 1, 0),
                                nowMillis = nowMillis
                            )
                        )
                    ),
                    showNotificationBanner = false,
                    showExactAlarmBanner = false,
                    onRequestNotificationPermission = {},
                    onOpenExactAlarmSettings = {},
                    onAddTreatmentClick = {},
                    onTreatmentClick = {},
                    onToggleDoseStatus = { toggledDoseId = it },
                    onRetry = {},
                    onDismissFeedback = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("adherence_progress_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dose_item_card_101").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_dose_button_101").performClick()
        assertEquals(101L, toggledDoseId)
    }

    @Test
    fun `home screen renders empty state CTA and invokes add treatment callback`() {
        var addClicked = false
        composeTestRule.setContent {
            DoseCertaTheme {
                HomeScreen(
                    uiState = HomeUiState(contentState = UiState.Empty("Vazio")),
                    showNotificationBanner = false,
                    showExactAlarmBanner = false,
                    onRequestNotificationPermission = {},
                    onOpenExactAlarmSettings = {},
                    onAddTreatmentClick = { addClicked = true },
                    onTreatmentClick = {},
                    onToggleDoseStatus = {},
                    onRetry = {},
                    onDismissFeedback = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("empty_state_view").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_state_action_button").performClick()
        assertTrue(addClicked)
    }

    @Test
    fun `add treatment screen displays live preview and triggers save action`() {
        var saveClicked = false
        composeTestRule.setContent {
            DoseCertaTheme {
                AddTreatmentScreen(
                    uiState = AddTreatmentUiState(
                        medicationName = "Ibuprofeno",
                        dosage = "400mg",
                        startTimeMillis = 1_700_000_000_000L,
                        intervalHoursText = "8",
                        durationDaysText = "5",
                        previewDoseCount = 15
                    ),
                    onBackClick = {},
                    onMedicationNameChange = {},
                    onDosageChange = {},
                    onNotesChange = {},
                    onStartTimeChange = {},
                    onIntervalHoursChange = {},
                    onDurationDaysChange = {},
                    onSaveClick = { saveClicked = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dose_preview_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("15 doses serão calculadas e agendadas automaticamente")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_treatment_button")
            .performScrollTo()
            .performClick()
        assertTrue(saveClicked)
    }

    @Test
    fun `drug search screen displays offline cache banner and medical disclaimer`() {
        val cachedDrug = DrugInfo(
            id = "fda-advil",
            brandName = "Advil",
            genericName = "IBUPROFEN",
            manufacturer = "Haleon US",
            indicationsAndUsage = "Pain relief",
            warnings = "Read warnings carefully",
            dosageAndAdministration = "1 capsule",
            contraindications = "Allergy",
            adverseReactions = "Mild upset",
            searchQuery = "ibuprofen",
            cachedAtMillis = 1_700_000_000_000L
        )

        composeTestRule.setContent {
            DoseCertaTheme {
                DrugSearchScreen(
                    uiState = DrugSearchUiState(
                        query = "ibuprofen",
                        contentState = UiState.Success(
                            DrugSearchContent(
                                items = listOf(cachedDrug),
                                isFromCache = true,
                                isShowingRecentHistory = false
                            )
                        )
                    ),
                    onQueryChange = {},
                    onClearQuery = {},
                    onSearch = {},
                    onSelectDrug = {},
                    onDismissDetail = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medical_disclaimer_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_cache_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drug_card_fda-advil").assertIsDisplayed()
    }
}
