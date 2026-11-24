package com.example.dosecerta.presentation.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.dosecerta.alarm.AndroidDoseAlarmScheduler
import com.example.dosecerta.alarm.NotificationHelper
import com.example.dosecerta.domain.model.TreatmentWithDoses
import com.example.dosecerta.presentation.common.DateTimeFormatters
import com.example.dosecerta.presentation.common.UiState
import com.example.dosecerta.presentation.components.AdherenceProgressCard
import com.example.dosecerta.presentation.components.DoseItemCard
import com.example.dosecerta.presentation.components.EmptyStateView
import com.example.dosecerta.presentation.components.ErrorStateView
import com.example.dosecerta.presentation.components.LoadingView
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun HomeRoute(
    onAddTreatmentClick: () -> Unit,
    onTreatmentClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    notificationHelper: NotificationHelper = koinInject(),
    alarmScheduler: AndroidDoseAlarmScheduler = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(notificationHelper.hasNotificationPermission())
    }
    var canScheduleExactAlarms by remember {
        mutableStateOf(alarmScheduler.canScheduleExactAlarms())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || notificationHelper.hasNotificationPermission()
    }

    LifecycleResumeEffect(Unit) {
        hasNotificationPermission = notificationHelper.hasNotificationPermission()
        canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()
        onPauseOrDispose {}
    }

    HomeScreen(
        uiState = uiState,
        showNotificationBanner = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission,
        showExactAlarmBanner = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms,
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenExactAlarmSettings = {
            alarmScheduler.createExactAlarmSettingsIntent()?.let { intent ->
                runCatching { context.startActivity(intent) }
            }
        },
        onAddTreatmentClick = onAddTreatmentClick,
        onTreatmentClick = onTreatmentClick,
        onToggleDoseStatus = viewModel::onToggleDoseStatus,
        onRetry = viewModel::observeSchedule,
        onDismissFeedback = viewModel::onDismissFeedback,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    showNotificationBanner: Boolean,
    showExactAlarmBanner: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onAddTreatmentClick: () -> Unit,
    onTreatmentClick: (Long) -> Unit,
    onToggleDoseStatus: (Long) -> Unit,
    onRetry: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onDismissFeedback()
        }
    }

    val todayHeader = remember {
        DateTimeFormatters.formatFullDateHeader(System.currentTimeMillis())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = todayHeader,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTreatmentClick,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_treatment)
                    )
                },
                text = { Text(text = stringResource(R.string.home_empty_cta)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_treatment_fab")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                if (showNotificationBanner) {
                    PermissionBanner(
                        icon = Icons.Outlined.NotificationsActive,
                        message = stringResource(R.string.home_permission_notifications_banner),
                        ctaText = stringResource(R.string.home_permission_notifications_cta),
                        onAction = onRequestNotificationPermission,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (showExactAlarmBanner) {
                    PermissionBanner(
                        icon = Icons.Outlined.Alarm,
                        message = stringResource(R.string.home_permission_exact_alarm_banner),
                        ctaText = stringResource(R.string.home_permission_exact_alarm_cta),
                        onAction = onOpenExactAlarmSettings,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                when (val state = uiState.contentState) {
                    UiState.Loading -> {
                        LoadingView(modifier = Modifier.fillMaxSize())
                    }

                    is UiState.Empty -> {
                        EmptyStateView(
                            title = stringResource(R.string.home_empty_title),
                            message = stringResource(R.string.home_empty_message),
                            illustrationRes = R.drawable.img_empty_schedule_1795521264462,
                            actionLabel = stringResource(R.string.home_empty_cta),
                            onActionClick = onAddTreatmentClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is UiState.Error -> {
                        ErrorStateView(
                            message = state.message,
                            canRetry = state.canRetry,
                            onRetry = onRetry,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is UiState.Success -> {
                        HomeSuccessList(
                            content = state.data,
                            onToggleDoseStatus = onToggleDoseStatus,
                            onTreatmentClick = onTreatmentClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSuccessList(
    content: HomeContent,
    onToggleDoseStatus: (Long) -> Unit,
    onTreatmentClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_schedule_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "adherence_card") {
            AdherenceProgressCard(
                title = stringResource(R.string.home_today_adherence),
                subtitle = stringResource(R.string.home_subtitle),
                adherence = content.todayAdherence
            )
        }

        item(key = "today_doses_header") {
            Text(
                text = stringResource(R.string.home_today_doses_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (content.todayDoses.isEmpty()) {
            item(key = "no_doses_today") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.home_no_doses_today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(
                items = content.todayDoses,
                key = { dose -> "today_dose_${dose.id}" }
            ) { dose ->
                DoseItemCard(
                    dose = dose,
                    nowMillis = content.nowMillis,
                    onToggleStatus = onToggleDoseStatus,
                    onCardClick = { onTreatmentClick(dose.treatmentId) }
                )
            }
        }

        item(key = "treatments_header") {
            Text(
                text = stringResource(R.string.home_treatments_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(
            items = content.treatments,
            key = { item -> "treatment_${item.treatment.id}" }
        ) { item ->
            TreatmentSummaryCard(
                item = item,
                onClick = { onTreatmentClick(item.treatment.id) }
            )
        }
    }
}

@Composable
private fun TreatmentSummaryCard(
    item: TreatmentWithDoses,
    onClick: () -> Unit
) {
    val treatment = item.treatment
    val adherence = item.adherence

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("treatment_card_${treatment.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = treatment.medicationName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = buildString {
                        if (treatment.dosage.isNotBlank()) {
                            append(treatment.dosage)
                            append(" • ")
                        }
                        append("A cada ${treatment.intervalHours}h • ${treatment.durationDays} dia(s)")
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Abrir detalhes de ${treatment.medicationName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { adherence.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${adherence.takenDoses} de ${adherence.totalDoses} doses tomadas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${adherence.adherencePercentage}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    ctaText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAction) {
                Text(
                    text = ctaText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
