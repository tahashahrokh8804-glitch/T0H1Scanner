package com.t0h1.scanner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.R
import com.t0h1.scanner.model.ScanSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: ScanSettings,
    onSettingsChanged: (ScanSettings) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.settings_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text(stringResource(R.string.settings_connection_title), style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = settings.timeoutSeconds.toString(),
                            onValueChange = { value ->
                                value.toDoubleOrNull()?.let {
                                    onSettingsChanged(settings.copy(timeoutSeconds = it.coerceIn(1.0, 30.0)))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.timeout_seconds_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )

                        OutlinedTextField(
                            value = settings.concurrency.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let {
                                    onSettingsChanged(settings.copy(concurrency = it.coerceIn(1, 64)))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.concurrency_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }

                    OutlinedTextField(
                        value = settings.maxDisplayedResults.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let {
                                onSettingsChanged(settings.copy(maxDisplayedResults = it.coerceIn(10, 1000)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.max_display_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                    OutlinedTextField(
                        value = settings.defaultPorts,
                        onValueChange = { onSettingsChanged(settings.copy(defaultPorts = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.default_ports_label)) },
                        placeholder = { Text(stringResource(R.string.ports_placeholder)) },
                    )
                }
            }
        }

        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.settings_info_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.settings_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.version_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.reset_defaults))
                }
            }
        }
    }
}
