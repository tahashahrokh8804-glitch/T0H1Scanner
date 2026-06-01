package com.t0h1.scanner.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.ScanViewModel
import com.t0h1.scanner.model.AppScreen
import com.t0h1.scanner.ui.screens.HomeScreen
import com.t0h1.scanner.ui.screens.SettingsScreen

@Composable
fun T0H1ScannerApp(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("T0H1 Scanner")
                        Text(
                            text = state.currentProfileHint,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.setScreen(
                            if (state.screen == AppScreen.Home) AppScreen.Settings else AppScreen.Home
                        )
                    }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (state.screen == AppScreen.Home) {
                HomeScreen(
                    state = state,
                    onTargetsChanged = viewModel::updateTargets,
                    onRegionFilterChanged = viewModel::updateRegionFilter,
                    onPortInputChanged = viewModel::updatePortInput,
                    onStart = viewModel::startScan,
                    onStop = viewModel::stopScan,
                    onExportCsv = viewModel::exportCsv,
                    onExportTxt = viewModel::exportTxt,
                    onExportJson = viewModel::exportJson,
                )
            } else {
                SettingsScreen(
                    settings = state.settings,
                    onSettingsChanged = viewModel::updateSettings,
                    onReset = viewModel::resetSettings,
                    onBack = { viewModel.setScreen(AppScreen.Home) },
                )
            }
        }
    }
}
