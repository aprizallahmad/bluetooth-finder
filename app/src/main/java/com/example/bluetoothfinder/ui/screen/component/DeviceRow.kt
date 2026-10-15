package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bluetoothfinder.model.BluetoothModel
import com.example.bluetoothfinder.ui.theme.signalColor

/**
 * Satu baris pada daftar perangkat:
 * Nama + MAC Address | RSSI mentah (dBm) | Estimasi jarak + kategori sinyal.
 */
@Composable
fun DeviceRow(
    device: BluetoothModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signalColor = signalColor(device.rssiValue)
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        BorderedCell(modifier = Modifier.weight(1.4f)) {
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        BorderedCell(
            modifier = Modifier.weight(0.8f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = device.rssi,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = signalColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        BorderedCell(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = device.distance,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = signalColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.signalCategory,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
