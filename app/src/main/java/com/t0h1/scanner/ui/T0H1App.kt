package com.t0h1.scanner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.R
import com.t0h1.scanner.ScanViewModel
import com.t0h1.scanner.model.AppScreen
import com.t0h1.scanner.ui.screens.HomeScreen
import com.t0h1.scanner.ui.screens.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun T0H1ScannerApp(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.app_name))
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
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings_accessibility),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                    onSearchChanged = viewModel::updateSearchQuery,
                    onStart = viewModel::startScan,
                    onStop = viewModel::stopScan,
                    onExportCsv = viewModel::exportCsv,
                    onExportTxt = viewModel::exportTxt,
                    onExportJson = viewModel::exportJson,
                    onClearResults = viewModel::clearResults,
                )
            } else {
                SettingsScreen(
                    settings = state.settings,
                    onSettingsChanged = { newSettings ->
                        viewModel.updateSettings { newSettings }
                    },
                    onReset = viewModel::resetSettings,
                    onBack = { viewModel.setScreen(AppScreen.Home) },
                )
            }
        }
    }
}
