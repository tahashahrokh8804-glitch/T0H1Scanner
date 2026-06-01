package com.t0h1.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t0h1.scanner.model.AppScreen
import com.t0h1.scanner.model.ScanResult
import com.t0h1.scanner.model.ScanSettings
import com.t0h1.scanner.model.ScanUiState
import com.t0h1.scanner.repository.ScannerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScannerRepository(application)

    private var scanJob: Job? = null

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(initialState())
    val uiState: kotlinx.coroutines.flow.StateFlow<ScanUiState> = _uiState

    init {
        val draft = repository.loadDraft()
        _uiState.value = _uiState.value.copy(
            targetInput = draft.targetInput,
            regionFilter = draft.regionFilter,
            portInput = draft.portInput,
            settings = draft.settings,
        )
    }

    fun setScreen(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(screen = screen)
    }

    fun updateTargets(value: String) {
        _uiState.value = _uiState.value.copy(targetInput = value)
        persistDraft()
    }

    fun updateRegionFilter(value: String) {
        _uiState.value = _uiState.value.copy(regionFilter = value)
        persistDraft()
    }

    fun updatePortInput(value: String) {
        _uiState.value = _uiState.value.copy(portInput = value)
        persistDraft()
    }

    fun updateSettings(transform: (ScanSettings) -> ScanSettings) {
        val updated = transform(_uiState.value.settings)
        _uiState.value = _uiState.value.copy(settings = updated)
        repository.updateSettings(updated)
        persistDraft()
    }

    fun resetSettings() {
        _uiState.value = _uiState.value.copy(settings = ScanSettings())
        repository.updateSettings(_uiState.value.settings)
        persistDraft()
    }

    fun startScan() {
        if (_uiState.value.isScanning) return

        val current = _uiState.value
        val cleanedTargets = current.targetInput.trim()
        val cleanedPorts = current.portInput.trim()

        if (cleanedTargets.isBlank()) {
            _uiState.value = current.copy(statusText = "Enter at least one target.")
            return
        }

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isScanning = true,
                    statusText = "Scanning...",
                    progressCompleted = 0,
                    progressTotal = 0,
                    responsive = 0,
                    results = emptyList(),
                    lastExportPath = "",
                    currentProfileHint = "Native scan in progress",
                )
                persistDraft()

                val results = repository.scan(
                    targetInput = cleanedTargets,
                    regionFilter = current.regionFilter,
                    portInput = cleanedPorts,
                    settings = current.settings,
                ) { completed, total, responsive ->
                    _uiState.value = _uiState.value.copy(
                        progressCompleted = completed,
                        progressTotal = total,
                        responsive = responsive,
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    statusText = if (results.isEmpty()) "No responsive targets found." else "Completed",
                    results = results,
                    progressCompleted = _uiState.value.progressTotal,
                    progressTotal = _uiState.value.progressTotal,
                    responsive = results.count { it.status.equals("open", true) || it.status.equals("success", true) },
                    currentProfileHint = "Ready",
                )
                persistDraft()
            } catch (cancelled: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    statusText = cancelled.message ?: "Scan stopped.",
                    currentProfileHint = "Ready",
                )
            }
        }
    }

    fun stopScan() {
        repository.stopScan()
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            statusText = "Stopping...",
            currentProfileHint = "Stopping",
        )
    }

    fun exportCsv() = export("csv")
    fun exportTxt() = export("txt")
    fun exportJson() = export("json")

    private fun export(format: String) {
        val results = _uiState.value.results
        if (results.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusText = "Nothing to export yet.")
            return
        }

        viewModelScope.launch {
            val file = repository.exportResults(results, format)
            _uiState.value = _uiState.value.copy(
                lastExportPath = file.absolutePath,
                statusText = "Exported ${format.uppercase()}",
            )
        }
    }

    private fun persistDraft() {
        val s = _uiState.value
        repository.saveDraft(
            targetInput = s.targetInput,
            regionFilter = s.regionFilter,
            portInput = s.portInput,
            settings = s.settings,
        )
    }

    private fun initialState(): ScanUiState = ScanUiState(
        screen = AppScreen.Home,
        targetInput = "",
        regionFilter = "",
        portInput = "",
        settings = ScanSettings(),
        isScanning = false,
        progressCompleted = 0,
        progressTotal = 0,
        responsive = 0,
        results = emptyList(),
        statusText = "Ready",
        lastExportPath = "",
        currentProfileHint = "Native Android UI • Chaquopy backend",
    )
}
