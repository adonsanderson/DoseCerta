package com.example.dosecerta.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.dosecerta.domain.model.AdherenceSummary
import com.example.dosecerta.domain.model.Dose
import com.example.dosecerta.domain.model.DoseStatus
import com.example.dosecerta.presentation.common.DateTimeFormatters
import com.example.dosecerta.presentation.theme.LocalDoseStatusColors

@Composable
fun AdherenceProgressCard(
    title: String,
    subtitle: String,
    adherence: AdherenceSummary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = adherence.progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "adherence_progress"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adherence_progress_card"),
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(primary, secondary)
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = onPrimary
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimary.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = onPrimary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "${adherence.adherencePercentage}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = onPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = onPrimary,
                    trackColor = onPrimary.copy(alpha = 0.25f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${adherence.takenDoses} de ${adherence.totalDoses} doses tomadas",
                        style = MaterialTheme.typography.labelMedium,
                        color = onPrimary.copy(alpha = 0.92f)
                    )
                    if (adherence.overdueDoses > 0) {
                        Text(
                            text = "${adherence.overdueDoses} atrasada(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    } else {
                        Text(
                            text = "${adherence.pendingDoses} pendente(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoseItemCard(
    dose: Dose,
    nowMillis: Long,
    onToggleStatus: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showFullDate: Boolean = false,
    onCardClick: (() -> Unit)? = null
) {
    val statusColors = LocalDoseStatusColors.current
    val isTaken = dose.status == DoseStatus.TAKEN
    val isOverdue = remember(dose.status, dose.scheduledTimeMillis, nowMillis) {
        dose.isOverdue(nowMillis)
    }
    val formattedSchedule = remember(dose.scheduledTimeMillis, showFullDate) {
        if (showFullDate) {
            DateTimeFormatters.formatShortDateTime(dose.scheduledTimeMillis)
        } else {
            DateTimeFormatters.formatTime(dose.scheduledTimeMillis)
        }
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isTaken -> statusColors.takenContainer.copy(alpha = 0.35f)
            isOverdue -> statusColors.overdueContainer.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "dose_card_bg"
    )

    val borderColor = when {
        isTaken -> statusColors.taken.copy(alpha = 0.35f)
        isOverdue -> statusColors.overdue.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            )
            .testTag("dose_item_card_${dose.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    isTaken -> statusColors.takenContainer
                    isOverdue -> statusColors.overdueContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedSchedule,
                        style = MaterialTheme.typography.titleSmall,
                        color = when {
                            isTaken -> statusColors.taken
                            isOverdue -> statusColors.overdue
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    Text(
                        text = "Dose #${dose.sequenceNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = dose.medicationName.ifBlank { "Medicamento" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                )
                if (dose.dosage.isNotBlank()) {
                    Text(
                        text = dose.dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusIcon = when {
                        isTaken -> Icons.Filled.CheckCircle
                        isOverdue -> Icons.Outlined.WarningAmber
                        else -> Icons.Outlined.AccessTime
                    }
                    val statusTint = when {
                        isTaken -> statusColors.taken
                        isOverdue -> statusColors.overdue
                        else -> MaterialTheme.colorScheme.primary
                    }
                    val statusText = when {
                        isTaken -> stringResource(R.string.status_taken)
                        isOverdue -> stringResource(R.string.status_overdue)
                        else -> stringResource(R.string.status_pending)
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusTint
                    )
                }
            }

            IconButton(
                onClick = { onToggleStatus(dose.id) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_dose_button_${dose.id}")
            ) {
                Icon(
                    imageVector = if (isTaken) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (isTaken) {
                        "Marcar ${dose.medicationName} como pendente"
                    } else {
                        "Marcar ${dose.medicationName} como tomada"
                    },
                    tint = if (isTaken) {
                        statusColors.taken
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
