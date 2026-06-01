package com.t0h1.scanner.model

enum class AppScreen {
    Home,
    Settings
}

data class ScanSettings(
    val timeoutSeconds: Double = 3.0,
    val concurrency: Int = 16,
    val maxDisplayedResults: Int = 200,
    val defaultPorts: String = "443,80,8080",
)

data class ScanTargetSpec(
    val host: String,
    val explicitPort: Int? = null,
    val region: String = "",
    val notes: String = "",
)

data class ScanResult(
    val status: String,
    val ip: String,
    val port: Int,
    val latencyMs: Double? = null,
    val notes: String = "",
    val target: String = "",
    val region: String = "",
)

data class AppDraft(
    val targetInput: String = "",
    val regionFilter: String = "",
    val portInput: String = "",
    val settings: ScanSettings = ScanSettings(),
)

data class ScanUiState(
    val screen: AppScreen = AppScreen.Home,
    val targetInput: String = "",
    val regionFilter: String = "",
    val portInput: String = "",
    val settings: ScanSettings = ScanSettings(),
    val isScanning: Boolean = false,
    val progressCompleted: Int = 0,
    val progressTotal: Int = 0,
    val responsive: Int = 0,
    val results: List<ScanResult> = emptyList(),
    val statusText: String = "Ready",
    val lastExportPath: String = "",
    val currentProfileHint: String = "Native Android UI • Chaquopy backend",
)
