package com.example.dosecerta.presentation.screens.drugsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.presentation.components.MedicalDisclaimerBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugDetailSheet(
    drug: DrugInfo,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("drug_detail_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = drug.brandName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${drug.genericName} • ${drug.manufacturer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            MedicalDisclaimerBanner()

            DrugLabelSection(
                title = stringResource(R.string.drugs_section_warnings),
                body = drug.warnings,
                isHighlighted = true
            )
            DrugLabelSection(
                title = stringResource(R.string.drugs_section_indications),
                body = drug.indicationsAndUsage
            )
            DrugLabelSection(
                title = stringResource(R.string.drugs_section_dosage),
                body = drug.dosageAndAdministration
            )
            DrugLabelSection(
                title = stringResource(R.string.drugs_section_contraindications),
                body = drug.contraindications
            )
            DrugLabelSection(
                title = stringResource(R.string.drugs_section_adverse),
                body = drug.adverseReactions
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_drug_detail_button")
            ) {
                Text(text = stringResource(R.string.drugs_close_sheet))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrugLabelSection(
    title: String,
    body: String,
    isHighlighted: Boolean = false
) {
    val resolvedBody = body.ifBlank { stringResource(R.string.drugs_not_available) }
    val containerColor = if (isHighlighted) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val titleColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor
            )
            Text(
                text = resolvedBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
