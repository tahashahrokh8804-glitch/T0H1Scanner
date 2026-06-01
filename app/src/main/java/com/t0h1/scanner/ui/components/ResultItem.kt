package com.t0h1.scanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.model.ScanResult

@Composable
fun ResultItem(result: ScanResult) {
    val status = result.status.lowercase()
    val statusColor = when {
        status.contains("open") || status.contains("success") -> Color(0xFF22C55E)
        status.contains("timeout") -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Circle, contentDescription = null, tint = statusColor)
                    Text(
                        text = result.status.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Text(
                    text = result.latencyMs?.let { "${it} ms" } ?: "--",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            Text(
                text = "${result.ip}:${result.port}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (result.target.isNotBlank()) {
                Text(
                    text = "Target: ${result.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.region.isNotBlank()) {
                Text(
                    text = "Region: ${result.region}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.notes.isNotBlank()) {
                Text(
                    text = result.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
