package com.t0h1.scanner.bridge

import com.chaquo.python.Python
import com.t0h1.scanner.model.ScanResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PythonScannerBridge {

    private val module by lazy { Python.getInstance().getModule("scanner.engine") }

    fun testLatency(host: String, port: Int, timeoutSeconds: Double): ScanResult {
        val raw = module.callAttr("test_latency", host, port, timeoutSeconds).toString()
        return jsonToResult(raw)
    }

    fun scanTargets(
        targetsJson: String,
        portsJson: String,
        regionFilter: String,
        timeoutSeconds: Double,
        concurrency: Int,
    ): List<ScanResult> {
        val raw = module.callAttr(
            "scan_targets",
            targetsJson,
            portsJson,
            regionFilter,
            timeoutSeconds,
            concurrency,
        ).toString()
        val array = JSONObject(raw).optJSONArray("results") ?: JSONArray()
        return (0 until array.length()).map { index ->
            jsonToScanResult(array.getJSONObject(index))
        }
    }

    fun filterByCountry(targetsJson: String, regionFilter: String): String {
        return module.callAttr("filter_by_country", targetsJson, regionFilter).toString()
    }

    fun exportResults(resultsJson: String, outputPath: String, format: String): File {
        val saved = module.callAttr("export_results", resultsJson, outputPath, format).toString()
        return File(saved)
    }

    private fun jsonToResult(rawJson: String): ScanResult {
        return jsonToScanResult(JSONObject(rawJson))
    }

    private fun jsonToScanResult(obj: JSONObject): ScanResult {
        return ScanResult(
            status = obj.optString("status", "error"),
            ip = obj.optString("ip", ""),
            port = obj.optInt("port", 0),
            latencyMs = if (obj.has("latency_ms") && !obj.isNull("latency_ms")) obj.optDouble("latency_ms") else null,
            notes = obj.optString("notes", ""),
            target = obj.optString("target", ""),
            region = obj.optString("region", ""),
        )
    }
}
