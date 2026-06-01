package com.t0h1.scanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.model.ScanUiState
import com.t0h1.scanner.ui.components.ResultItem

@Composable
fun HomeScreen(
    state: ScanUiState,
    onTargetsChanged: (String) -> Unit,
    onRegionFilterChanged: (String) -> Unit,
    onPortInputChanged: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExportCsv: () -> Unit,
    onExportTxt: () -> Unit,
    onExportJson: () -> Unit,
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Targets", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.targetInput,
                    onValueChange = onTargetsChanged,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    label = { Text("Target list") },
                    placeholder = { Text("1.1.1.1\nexample.com\nexample.com:443 | US | primary") },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.regionFilter,
                        onValueChange = onRegionFilterChanged,
                        modifier = Modifier.weight(1f),
                        label = { Text("Country / region filter") },
                        placeholder = { Text("US, EU, Asia, primary") },
                    )
                    OutlinedTextField(
                        value = state.portInput,
                        onValueChange = onPortInputChanged,
                        modifier = Modifier.weight(1f),
                        label = { Text("Port list") },
                        placeholder = { Text("80, 443, 8080") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onStart,
                        enabled = !state.isScanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start scan")
                    }

                    OutlinedButton(
                        onClick = onStop,
                        enabled = state.isScanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilterChip(
                        selected = state.isScanning,
                        onClick = {},
                        label = { Text(if (state.isScanning) "Scanning" else "Idle") },
                    )
                    FilterChip(
                        selected = false,
                        onClick = onExportCsv,
                        label = { Text("Export CSV") },
                    )
                    FilterChip(
                        selected = false,
                        onClick = onExportTxt,
                        label = { Text("Export TXT") },
                    )
                    FilterChip(
                        selected = false,
                        onClick = onExportJson,
                        label = { Text("Export JSON") },
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Progress", style = MaterialTheme.typography.titleMedium)
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
                    Text("Completed: ${state.progressCompleted}")
                    Text("Total: ${state.progressTotal}")
                    Text("Responsive: ${state.responsive}")
                }
                Text(
                    text = state.statusText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.lastExportPath.isNotBlank()) {
                    Text(
                        text = "Last export: ${state.lastExportPath}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Results", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Status • Latency • IP • Port • Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No results yet. Run a scan to see entries here.")
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.results.forEach { result ->
                            ResultItem(result = result)
                        }
                    }
                }
            }
        }
    }
}
