package com.t0h1.scanner.repository

import android.content.Context
import com.t0h1.scanner.bridge.PythonScannerBridge
import com.t0h1.scanner.model.AppDraft
import com.t0h1.scanner.model.ScanResult
import com.t0h1.scanner.model.ScanSettings
import com.t0h1.scanner.model.ScanTargetSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class ScannerRepository(private val context: Context) {

    private val bridge = PythonScannerBridge()
    private val stopRequested = AtomicBoolean(false)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadDraft(): AppDraft {
        return AppDraft(
            targetInput = prefs.getString(KEY_TARGETS, "") ?: "",
            regionFilter = prefs.getString(KEY_REGION, "") ?: "",
            portInput = prefs.getString(KEY_PORTS, "") ?: "",
            searchQuery = prefs.getString(KEY_SEARCH, "") ?: "",
            settings = ScanSettings(
                timeoutSeconds = prefs.getString(KEY_TIMEOUT, "3.0")?.toDoubleOrNull() ?: 3.0,
                concurrency = prefs.getString(KEY_CONCURRENCY, "16")?.toIntOrNull() ?: 16,
                maxDisplayedResults = prefs.getString(KEY_MAX_RESULTS, "200")?.toIntOrNull() ?: 200,
                defaultPorts = prefs.getString(KEY_DEFAULT_PORTS, "443,80,8080") ?: "443,80,8080",
            ),
        )
    }

    fun saveDraft(targetInput: String, regionFilter: String, portInput: String, searchQuery: String, settings: ScanSettings) {
        prefs.edit()
            .putString(KEY_TARGETS, targetInput)
            .putString(KEY_REGION, regionFilter)
            .putString(KEY_PORTS, portInput)
            .putString(KEY_SEARCH, searchQuery)
            .putString(KEY_TIMEOUT, settings.timeoutSeconds.toString())
            .putString(KEY_CONCURRENCY, settings.concurrency.toString())
            .putString(KEY_MAX_RESULTS, settings.maxDisplayedResults.toString())
            .putString(KEY_DEFAULT_PORTS, settings.defaultPorts)
            .apply()
    }

    fun updateSettings(settings: ScanSettings) {
        prefs.edit()
            .putString(KEY_TIMEOUT, settings.timeoutSeconds.toString())
            .putString(KEY_CONCURRENCY, settings.concurrency.toString())
            .putString(KEY_MAX_RESULTS, settings.maxDisplayedResults.toString())
            .putString(KEY_DEFAULT_PORTS, settings.defaultPorts)
            .apply()
    }

    fun loadSettings(): ScanSettings = loadDraft().settings

    fun stopScan() {
        stopRequested.set(true)
    }

    fun resetStop() {
        stopRequested.set(false)
    }

    fun parseTargets(raw: String, regionFilter: String): List<ScanTargetSpec> {
        val filter = regionFilter.trim()
        return raw.lineSequence()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith(";")) return@mapNotNull null

                val parts = trimmed.split("|").map { it.trim() }
                val endpointPart = parts.getOrNull(0).orEmpty()
                val region = parts.getOrNull(1).orEmpty()
                val notes = parts.getOrNull(2).orEmpty()

                if (filter.isNotBlank()) {
                    val hay = listOf(endpointPart, region, notes).joinToString(" ")
                    if (!hay.contains(filter, ignoreCase = true)) return@mapNotNull null
                }

                val parsed = parseEndpoint(endpointPart) ?: return@mapNotNull null
                ScanTargetSpec(
                    host = parsed.first,
                    explicitPort = parsed.second,
                    region = region,
                    notes = notes,
                )
            }
            .toList()
    }

    fun parsePorts(raw: String, fallback: String): List<Int> {
    val source = if (raw.isBlank()) fallback else raw

    return source
        .split(',', '\n', ';', ' ')
        .mapNotNull { token ->
            val value = token.trim()
            if (value.isBlank()) null else value.toIntOrNull()
        }
        .filter { it in 1..65535 }
        .distinct()
}

    fun buildEndpoints(targets: List<ScanTargetSpec>, ports: List<Int>): List<ScanEndpoint> {
        val out = mutableListOf<ScanEndpoint>()
        val seen = linkedSetOf<String>()
        for (target in targets) {
            if (target.explicitPort != null) {
                val key = "${target.host}:${target.explicitPort}"
                if (seen.add(key)) {
                    out += ScanEndpoint(target.host, target.explicitPort, target.region, target.notes)
                }
            } else {
                for (port in ports) {
                    val key = "${target.host}:$port"
                    if (seen.add(key)) {
                        out += ScanEndpoint(target.host, port, target.region, target.notes)
                    }
                }
            }
        }
        return out
    }

    suspend fun scan(
        targetInput: String,
        regionFilter: String,
        portInput: String,
        settings: ScanSettings,
        onProgress: (completed: Int, total: Int, responsive: Int) -> Unit,
    ): List<ScanResult> = coroutineScope {
        resetStop()

        val targets = parseTargets(targetInput, regionFilter)
        val ports = parsePorts(portInput, settings.defaultPorts)
        val endpoints = buildEndpoints(targets, ports)

        if (endpoints.isEmpty()) {
            onProgress(0, 0, 0)
            return@coroutineScope emptyList<ScanResult>()
        }

        val jsonTargets = targetsToJson(targets)
        bridge.filterByCountry(jsonTargets, regionFilter)
        val total = endpoints.size
        val completed = java.util.concurrent.atomic.AtomicInteger(0)
        val responsive = java.util.concurrent.atomic.AtomicInteger(0)
        val results = Collections.synchronizedList(mutableListOf<ScanResult>())
        val semaphore = Semaphore(max(1, settings.concurrency))

        val jobs = endpoints.map { endpoint ->
            async(Dispatchers.IO) {
                if (stopRequested.get()) return@async

                semaphore.withPermit {
                    if (stopRequested.get()) return@withPermit

                    val result = runCatching {
                        bridge.testLatency(endpoint.host, endpoint.port, settings.timeoutSeconds)
                    }.getOrElse { throwable ->
                        ScanResult(
                            status = "error",
                            ip = endpoint.host,
                            port = endpoint.port,
                            latencyMs = null,
                            notes = throwable.message ?: "unknown error",
                            target = endpoint.host,
                            region = endpoint.region,
                        )
                    }

                    val merged = result.copy(
                        target = endpoint.host,
                        region = endpoint.region.ifBlank { result.region },
                        notes = listOf(endpoint.notes, result.notes).filter { it.isNotBlank() }.joinToString(" | "),
                    )

                    if (merged.status.equals("open", ignoreCase = true) || merged.status.equals("success", ignoreCase = true)) {
                        responsive.incrementAndGet()
                    }

                    results += merged
                    val done = completed.incrementAndGet()
                    onProgress(done, total, responsive.get())
                }
            }
        }

        jobs.forEach { it.await() }

        val sorted = results.toList().sortedWith(
            compareByDescending<ScanResult> { it.status.equals("open", ignoreCase = true) || it.status.equals("success", ignoreCase = true) }
                .thenBy { it.latencyMs ?: Double.MAX_VALUE }
                .thenBy { it.ip }
                .thenBy { it.port }
        )

        sorted.take(settings.maxDisplayedResults)
    }

    fun exportResults(results: List<ScanResult>, format: String): File {
        val exportDir = getExportDir()
        exportDir.mkdirs()

        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = when (format.lowercase()) {
            "txt" -> File(exportDir, "t0h1_scanner_$stamp.txt")
            "json" -> File(exportDir, "t0h1_scanner_$stamp.json")
            else -> File(exportDir, "t0h1_scanner_$stamp.csv")
        }

        val payload = resultsToJson(results)
        val saved = bridge.exportResults(payload, file.absolutePath, format)
        return saved
    }

    private fun getExportDir(): File {
        val external = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
        return external?.let { File(it, "T0H1 Scanner") } ?: File(context.filesDir, "exports")
    }

    private fun parseEndpoint(text: String): Pair<String, Int?>? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            val host = trimmed.substring(1, trimmed.indexOf("]")).trim()
            val tail = trimmed.substring(trimmed.indexOf("]") + 1).trim()
            val port = if (tail.startsWith(":")) tail.drop(1).toIntOrNull() else null
            return host to port
        }

        if (trimmed.contains(",")) {
            val parts = trimmed.split(",").map { it.trim() }
            if (parts.size >= 2) {
                val port = parts.last().toIntOrNull()
                if (port != null) {
                    return parts.dropLast(1).joinToString(",").trim() to port
                }
            }
        }

        val colonCount = trimmed.count { it == ':' }
        if (colonCount == 1 && !trimmed.contains("://")) {
            val host = trimmed.substringBeforeLast(":").trim()
            val port = trimmed.substringAfterLast(":").toIntOrNull()
            if (port != null) return host to port
        }

        return trimmed to null
    }

    private fun targetsToJson(targets: List<ScanTargetSpec>): String {
        val array = JSONArray()
        for (target in targets) {
            val obj = org.json.JSONObject()
            obj.put("host", target.host)
            obj.put("explicit_port", target.explicitPort)
            obj.put("region", target.region)
            obj.put("notes", target.notes)
            array.put(obj)
        }
        return array.toString()
    }

    private fun resultsToJson(results: List<ScanResult>): String {
        val array = JSONArray()
        for (result in results) {
            val obj = org.json.JSONObject()
            obj.put("status", result.status)
            obj.put("ip", result.ip)
            obj.put("port", result.port)
            if (result.latencyMs != null) obj.put("latency_ms", result.latencyMs) else obj.put("latency_ms", org.json.JSONObject.NULL)
            obj.put("notes", result.notes)
            obj.put("target", result.target)
            obj.put("region", result.region)
            array.put(obj)
        }
        return array.toString()
    }

    data class ScanEndpoint(
        val host: String,
        val port: Int,
        val region: String = "",
        val notes: String = "",
    )

    companion object {
        private const val PREFS_NAME = "t0h1_scanner_prefs"
        private const val KEY_TARGETS = "targets"
        private const val KEY_REGION = "region"
        private const val KEY_PORTS = "ports"
        private const val KEY_SEARCH = "search"
        private const val KEY_TIMEOUT = "timeout"
        private const val KEY_CONCURRENCY = "concurrency"
        private const val KEY_MAX_RESULTS = "max_results"
        private const val KEY_DEFAULT_PORTS = "default_ports"
    }
}
