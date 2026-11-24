package com.example.dosecerta.presentation.screens.treatmentdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.dosecerta.presentation.common.DateTimeFormatters
import com.example.dosecerta.presentation.common.UiState
import com.example.dosecerta.presentation.components.AdherenceProgressCard
import com.example.dosecerta.presentation.components.DoseItemCard
import com.example.dosecerta.presentation.components.EmptyStateView
import com.example.dosecerta.presentation.components.ErrorStateView
import com.example.dosecerta.presentation.components.LoadingView
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TreatmentDetailRoute(
    treatmentId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TreatmentDetailViewModel = koinViewModel(
        key = "treatment_detail_$treatmentId",
        parameters = { parametersOf(treatmentId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBackClick()
        }
    }

    TreatmentDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onToggleDoseStatus = viewModel::onToggleDoseStatus,
        onDeleteTreatment = viewModel::onDeleteTreatment,
        onRetry = viewModel::observeTreatment,
        onDismissFeedback = viewModel::onDismissFeedback,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TreatmentDetailScreen(
    uiState: TreatmentDetailUiState,
    onBackClick: () -> Unit,
    onToggleDoseStatus: (Long) -> Unit,
    onDeleteTreatment: () -> Unit,
    onRetry: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onDismissFeedback()
        }
    }

    val topBarTitle = when (val state = uiState.contentState) {
        is UiState.Success -> state.data.treatment.medicationName
        else -> stringResource(R.string.detail_title)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (uiState.contentState is UiState.Success) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !uiState.isDeleting,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_treatment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.detail_delete_action),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                when (val state = uiState.contentState) {
                    UiState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())

                    is UiState.Empty -> EmptyStateView(
                        title = stringResource(R.string.detail_title),
                        message = stringResource(R.string.detail_not_found),
                        icon = Icons.Outlined.EventBusy,
                        actionLabel = stringResource(R.string.cd_back),
                        onActionClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )

                    is UiState.Error -> ErrorStateView(
                        message = state.message,
                        canRetry = state.canRetry,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize()
                    )

                    is UiState.Success -> {
                        val detail = state.data
                        val treatment = detail.treatment
                        val startFormatted = remember(treatment.startTimeMillis) {
                            DateTimeFormatters.formatShortDateTime(treatment.startTimeMillis)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("treatment_detail_doses_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "detail_adherence") {
                                AdherenceProgressCard(
                                    title = treatment.medicationName,
                                    subtitle = treatment.dosage.ifBlank {
                                        "A cada ${treatment.intervalHours}h por ${treatment.durationDays} dia(s)"
                                    },
                                    adherence = detail.adherence
                                )
                            }

                            item(key = "detail_meta") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Início: $startFormatted") }
                                        )
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Intervalo: ${treatment.intervalHours}h") }
                                        )
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Duração: ${treatment.durationDays} dia(s)") }
                                        )
                                    }

                                    if (treatment.notes.isNotBlank()) {
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Observações: ${treatment.notes}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            item(key = "detail_schedule_header") {
                                Text(
                                    text = stringResource(R.string.detail_schedule_section),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(
                                items = detail.doses,
                                key = { dose -> "detail_dose_${dose.id}" }
                            ) { dose ->
                                DoseItemCard(
                                    dose = dose,
                                    nowMillis = uiState.nowMillis,
                                    onToggleStatus = onToggleDoseStatus,
                                    showFullDate = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteTreatment()
                    },
                    modifier = Modifier.testTag("confirm_delete_treatment_button")
                ) {
                    Text(
                        text = stringResource(R.string.detail_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}
