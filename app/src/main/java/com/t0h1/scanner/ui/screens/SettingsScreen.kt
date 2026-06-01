package com.t0h1.scanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.model.ScanSettings

@Composable
fun SettingsScreen(
    settings: ScanSettings,
    onSettingsChanged: (ScanSettings) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ElevatedCard(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text("Settings", style = MaterialTheme.typography.titleLarge)
                }

                OutlinedTextField(
                    value = settings.timeoutSeconds.toString(),
                    onValueChange = { value ->
                        value.toDoubleOrNull()?.let { onSettingsChanged(settings.copy(timeoutSeconds = it.coerceIn(1.0, 30.0))) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Timeout (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )

                OutlinedTextField(
                    value = settings.concurrency.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onSettingsChanged(settings.copy(concurrency = it.coerceIn(1, 64))) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Concurrency") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                OutlinedTextField(
                    value = settings.maxDisplayedResults.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { onSettingsChanged(settings.copy(maxDisplayedResults = it.coerceIn(10, 1000))) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Max displayed results") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                OutlinedTextField(
                    value = settings.defaultPorts,
                    onValueChange = { onSettingsChanged(settings.copy(defaultPorts = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Default ports") },
                    placeholder = { Text("443,80,8080") },
                )

                Text(
                    text = "Python 3.13 runs inside the app through Chaquopy. The scanner stays native and offline until you start a scan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Reset defaults")
                    }
                }
            }
        }
    }
}
