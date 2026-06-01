package com.t0h1.scanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t0h1.scanner.R
import com.t0h1.scanner.model.ScanResult
import java.util.Locale

@Composable
fun ResultItem(result: ScanResult, modifier: Modifier = Modifier) {
    val status = result.status.lowercase(Locale.getDefault())
    val statusColor = when {
        status.contains("open") || status.contains("success") -> Color(0xFF22C55E)
        status.contains("timeout") -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    contentColor = statusColor,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = statusLabel(status),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }

                Text(
                    text = result.latencyMs?.let { "${String.format(Locale.US, "%.2f", it)} ${stringResource(R.string.ms_short)}" } ?: "--",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "${result.ip}:${result.port}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (result.target.isNotBlank()) {
                Text(
                    text = "${stringResource(R.string.target_label)}: ${result.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (result.region.isNotBlank()) {
                Text(
                    text = "${stringResource(R.string.region_label)}: ${result.region}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (result.notes.isNotBlank()) {
                Text(
                    text = "${stringResource(R.string.notes_label)}: ${result.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: String): String = when {
    status.contains("open") -> stringResource(R.string.status_open)
    status.contains("success") -> stringResource(R.string.status_success)
    status.contains("timeout") -> stringResource(R.string.status_timeout)
    status.contains("closed") -> stringResource(R.string.status_closed)
    else -> stringResource(R.string.status_error)
}
