package com.t0h1.scanner

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t0h1.scanner.model.AppScreen
import com.t0h1.scanner.model.ScanResult
import com.t0h1.scanner.model.ScanSettings
import com.t0h1.scanner.model.ScanUiState
import com.t0h1.scanner.repository.ScannerRepository
import kotlinx.coroutines.Job
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
            searchQuery = draft.searchQuery,
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

    fun updateSearchQuery(value: String) {
        _uiState.value = _uiState.value.copy(searchQuery = value)
        persistDraft()
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            results = emptyList(),
            progressCompleted = 0,
            progressTotal = 0,
            responsive = 0,
            statusText = text(R.string.status_ready),
            currentProfileHint = text(R.string.hero_hint),
            lastExportPath = "",
        )
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
            _uiState.value = current.copy(statusText = text(R.string.status_no_targets))
            return
        }

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isScanning = true,
                    statusText = text(R.string.status_scanning),
                    progressCompleted = 0,
                    progressTotal = 0,
                    responsive = 0,
                    results = emptyList(),
                    lastExportPath = "",
                    currentProfileHint = text(R.string.scanning),
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
                    statusText = if (results.isEmpty()) text(R.string.status_no_responsive) else text(R.string.status_completed),
                    results = results,
                    progressCompleted = _uiState.value.progressTotal,
                    progressTotal = _uiState.value.progressTotal,
                    responsive = results.count { it.status.equals("open", true) || it.status.equals("success", true) },
                    currentProfileHint = text(R.string.status_ready),
                )
                persistDraft()
            } catch (cancelled: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    statusText = cancelled.message ?: text(R.string.status_stopping),
                    currentProfileHint = text(R.string.status_ready),
                )
            }
        }
    }

    fun stopScan() {
        repository.stopScan()
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isScanning = false,
            statusText = text(R.string.status_stopping),
            currentProfileHint = text(R.string.status_stopping),
        )
    }

    fun exportCsv() = export("csv")
    fun exportTxt() = export("txt")
    fun exportJson() = export("json")

    private fun export(format: String) {
        val results = _uiState.value.results
        if (results.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusText = text(R.string.status_nothing_export))
            return
        }

        viewModelScope.launch {
            val file = repository.exportResults(results, format)
            _uiState.value = _uiState.value.copy(
                lastExportPath = file.absolutePath,
                statusText = text(R.string.status_exported, format.uppercase()),
            )
        }
    }

    private fun persistDraft() {
        val s = _uiState.value
        repository.saveDraft(
            targetInput = s.targetInput,
            regionFilter = s.regionFilter,
            portInput = s.portInput,
            searchQuery = s.searchQuery,
            settings = s.settings,
        )
    }

    private fun initialState(): ScanUiState = ScanUiState(
        screen = AppScreen.Home,
        targetInput = "",
        regionFilter = "",
        portInput = "",
        searchQuery = "",
        settings = ScanSettings(),
        isScanning = false,
        progressCompleted = 0,
        progressTotal = 0,
        responsive = 0,
        results = emptyList(),
        statusText = text(R.string.status_ready),
        lastExportPath = "",
        currentProfileHint = text(R.string.hero_hint),
    )

    private fun text(@StringRes resId: Int, vararg args: Any): String {
        return if (args.isEmpty()) {
            getApplication<Application>().getString(resId)
        } else {
            getApplication<Application>().getString(resId, *args)
        }
    }
}
