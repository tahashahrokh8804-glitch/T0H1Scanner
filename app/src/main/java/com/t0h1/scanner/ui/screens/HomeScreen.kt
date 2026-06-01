package com.t0h1.scanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.R
import com.t0h1.scanner.model.ScanUiState
import com.t0h1.scanner.ui.components.MetricCard
import com.t0h1.scanner.ui.components.ResultItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ScanUiState,
    onTargetsChanged: (String) -> Unit,
    onRegionFilterChanged: (String) -> Unit,
    onPortInputChanged: (String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExportCsv: () -> Unit,
    onExportTxt: () -> Unit,
    onExportJson: () -> Unit,
    onClearResults: () -> Unit,
) {
    val visibleResults = remember(state.results, state.searchQuery, state.settings.maxDisplayedResults) {
        filterResults(state.results, state.searchQuery, state.settings.maxDisplayedResults)
    }

    val responsiveVisible = visibleResults.count { isResponsive(it.status) }
    val averageLatency = visibleResults.mapNotNull { it.latencyMs }.takeIf { it.isNotEmpty() }?.average()
    val bestLatency = visibleResults.mapNotNull { it.latencyMs }.minOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.hero_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.currentProfileHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(if (state.isScanning) stringResource(R.string.scanning) else stringResource(R.string.idle))
                                },
                            )
                            AssistChip(
                                onClick = onExportCsv,
                                label = { Text(stringResource(R.string.export_csv)) },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = onExportTxt,
                                label = { Text(stringResource(R.string.export_txt)) },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                            )
                            AssistChip(
                                onClick = onExportJson,
                                label = { Text(stringResource(R.string.export_json)) },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onStart,
                            enabled = !state.isScanning,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.start_scan))
                        }
                        OutlinedButton(
                            onClick = onStop,
                            enabled = state.isScanning,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.stop_scan))
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.targets_title), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.targetInput,
                        onValueChange = onTargetsChanged,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        label = { Text(stringResource(R.string.targets_label)) },
                        placeholder = { Text(stringResource(R.string.targets_placeholder)) },
                    )
                    Text(
                        text = stringResource(R.string.targets_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = state.regionFilter,
                            onValueChange = onRegionFilterChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.region_filter_label)) },
                            placeholder = { Text(stringResource(R.string.region_filter_placeholder)) },
                        )
                        OutlinedTextField(
                            value = state.portInput,
                            onValueChange = onPortInputChanged,
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.ports_label)) },
                            placeholder = { Text(stringResource(R.string.ports_placeholder)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    label = stringResource(R.string.results_title),
                    value = visibleResults.size.toString(),
                    detail = stringResource(R.string.results_displayed),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.responsive),
                    value = responsiveVisible.toString(),
                    detail = stringResource(R.string.status_open),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.average_latency),
                    value = averageLatency?.let { String.format(Locale.US, "%.2f %s", it, stringResource(R.string.ms_short)) } ?: "--",
                    detail = bestLatency?.let { "${stringResource(R.string.best_latency)}: ${String.format(Locale.US, "%.2f", it)} ${stringResource(R.string.ms_short)}" },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.progress), style = MaterialTheme.typography.titleMedium)
                    val progressValue = when {
                        state.progressTotal <= 0 -> 0f
                        else -> state.progressCompleted.toFloat() / state.progressTotal.toFloat()
                    }
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${stringResource(R.string.completed)}: ${state.progressCompleted}")
                        Text("${stringResource(R.string.total)}: ${state.progressTotal}")
                        Text("${stringResource(R.string.responsive)}: ${state.responsive}")
                    }
                    Text(
                        text = state.statusText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.lastExportPath.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.last_export)}: ${state.lastExportPath}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.results_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.open_results_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onClearResults) {
                            Icon(Icons.Rounded.Clear, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.clear_results))
                        }
                    }

                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.search_label)) },
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (state.searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { onSearchChanged("") }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = null)
                                }
                            }
                        } else null,
                    )
                }
            }
        }

        if (visibleResults.isEmpty()) {
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.results_empty),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.results_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(visibleResults, key = { "${it.ip}:${it.port}:${it.target}" }) { result ->
                ResultItem(result = result)
            }
        }
    }
}

private fun filterResults(
    results: List<com.t0h1.scanner.model.ScanResult>,
    query: String,
    limit: Int,
): List<com.t0h1.scanner.model.ScanResult> {
    val normalized = query.trim().lowercase(Locale.getDefault())
    val filtered = if (normalized.isBlank()) {
        results
    } else {
        results.filter { result ->
            listOf(result.status, result.ip, result.port.toString(), result.target, result.region, result.notes)
                .joinToString(" ")
                .lowercase(Locale.getDefault())
                .contains(normalized)
        }
    }
    return filtered.take(limit.coerceAtLeast(1))
}

private fun isResponsive(status: String): Boolean {
    val value = status.lowercase(Locale.getDefault())
    return value.contains("open") || value.contains("success")
}
