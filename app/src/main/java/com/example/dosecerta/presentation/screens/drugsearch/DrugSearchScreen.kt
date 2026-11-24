package com.example.dosecerta.presentation.screens.drugsearch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.dosecerta.domain.model.DrugInfo
import com.example.dosecerta.presentation.common.UiState
import com.example.dosecerta.presentation.components.EmptyStateView
import com.example.dosecerta.presentation.components.ErrorStateView
import com.example.dosecerta.presentation.components.LoadingView
import com.example.dosecerta.presentation.components.MedicalDisclaimerBanner
import com.example.dosecerta.presentation.components.OfflineCacheBanner
import org.koin.androidx.compose.koinViewModel

private val QUICK_SEARCH_SUGGESTIONS = listOf(
    "Ibuprofen",
    "Amoxicillin",
    "Acetaminophen",
    "Aspirin"
)

@Composable
fun DrugSearchRoute(
    modifier: Modifier = Modifier,
    viewModel: DrugSearchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DrugSearchScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onSearch = viewModel::onSearch,
        onSelectDrug = viewModel::onSelectDrug,
        onDismissDetail = viewModel::onDismissDetail,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DrugSearchScreen(
    uiState: DrugSearchUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit,
    onSelectDrug: (DrugInfo) -> Unit,
    onDismissDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.drugs_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.drugs_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.drugs_search_placeholder),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearQuery,
                                    modifier = Modifier.testTag("clear_drug_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = "Limpar busca"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                onSearch()
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drug_search_input")
                    )

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onSearch()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("drug_search_button")
                    ) {
                        Text(text = stringResource(R.string.drugs_search_button))
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QUICK_SEARCH_SUGGESTIONS.forEach { suggestion ->
                        AssistChip(
                            onClick = {
                                onQueryChange(suggestion)
                                keyboardController?.hide()
                                onSearch()
                            },
                            label = { Text(suggestion) },
                            modifier = Modifier.testTag("suggestion_chip_$suggestion")
                        )
                    }
                }

                MedicalDisclaimerBanner()

                when (val state = uiState.contentState) {
                    UiState.Loading -> {
                        LoadingView(
                            message = "Consultando OpenFDA...",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    is UiState.Empty -> {
                        val isSearchEmpty = uiState.query.trim().length >= 2
                        EmptyStateView(
                            title = if (isSearchEmpty) {
                                stringResource(R.string.drugs_empty_search_title)
                            } else {
                                stringResource(R.string.drugs_empty_cache_title)
                            },
                            message = if (isSearchEmpty) {
                                stringResource(R.string.drugs_empty_search_message)
                            } else {
                                stringResource(R.string.drugs_empty_cache_message)
                            },
                            icon = Icons.Outlined.MenuBook,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    is UiState.Error -> {
                        ErrorStateView(
                            message = state.message,
                            canRetry = state.canRetry,
                            onRetry = onSearch,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    is UiState.Success -> {
                        val content = state.data
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("drug_results_list"),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (content.isFromCache && !content.isShowingRecentHistory) {
                                item(key = "offline_cache_banner") {
                                    OfflineCacheBanner()
                                }
                            }

                            item(key = "section_header") {
                                Text(
                                    text = if (content.isShowingRecentHistory) {
                                        stringResource(R.string.drugs_recent_cache_title)
                                    } else {
                                        stringResource(R.string.drugs_search_results_title)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            items(
                                items = content.items,
                                key = { drug -> drug.id }
                            ) { drug ->
                                DrugInfoCard(
                                    drug = drug,
                                    isFromCache = content.isFromCache,
                                    onClick = { onSelectDrug(drug) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.selectedDrug?.let { selected ->
        DrugDetailSheet(
            drug = selected,
            onDismiss = onDismissDetail
        )
    }
}

@Composable
private fun DrugInfoCard(
    drug: DrugInfo,
    isFromCache: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("drug_card_${drug.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drug.brandName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = drug.genericName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isFromCache) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFromCache) {
                                Icons.Outlined.OfflinePin
                            } else {
                                Icons.Outlined.CloudDone
                            },
                            contentDescription = null,
                            tint = if (isFromCache) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isFromCache) "Offline" else "OpenFDA",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFromCache) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }
            }

            val previewSnippet = drug.warnings.ifBlank { drug.indicationsAndUsage }
            if (previewSnippet.isNotBlank()) {
                Text(
                    text = previewSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Fabricante: ${drug.manufacturer}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
