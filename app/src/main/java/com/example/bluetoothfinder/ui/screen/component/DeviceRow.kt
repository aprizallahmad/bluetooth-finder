package com.example.bluetoothfinder.ui.screen.component

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.bluetoothfinder.model.BluetoothModel
import com.example.bluetoothfinder.ui.theme.signalColor
import com.example.bluetoothfinder.util.DeviceLog

/**
 * Satu baris pada daftar perangkat:
 * Nama + MAC Address | RSSI mentah (dBm) | Estimasi jarak + kategori sinyal.
 *
 * Garis tabel digambar sekali pada baris ([deviceTableGrid]) agar lurus dan tidak ganda.
 *
 * Perangkat **hilang sinyal** ([BluetoothModel.isStale]) tetap ditampilkan (retensi sesi)
 * tapi dirender redup dan kolom jarak diganti "Terakhir terlihat: ... lalu". Saat paket
 * advertising kembali masuk, baris otomatis nyala lagi.
 */
@Composable
fun DeviceRow(
    device: BluetoothModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Warna mengikuti RSSI halus (EMA) agar indikator tidak berkedip tiap paket.
    val deviceSignalColor = signalColor(device.indicatorRssi)
    val colorScheme = MaterialTheme.colorScheme

    // Baris hilang: sinyal & teks dibuat redup (alpha rendah) sebagai penanda visual.
    val stale = device.isStale
    val signalColor = if (stale) deviceSignalColor.copy(alpha = 0.35f) else deviceSignalColor
    val nameColor = if (stale) {
        colorScheme.onSurface.copy(alpha = 0.45f)
    } else {
        colorScheme.onSurface
    }

    // Logging Timber (hanya build debug): cetak object saat baris ini dirender di LazyColumn.
    // SideEffect -> berjalan sekali per set-composition baris, tidak mengulang tiap recomposition.
    SideEffect {
        DeviceLog.logDeviceRender(device)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .deviceTableGrid()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableCell(modifier = Modifier.weight(DeviceTableWeights[0])) {
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.copy(
                        alpha = if (stale) 0.45f else colorScheme.onSurfaceVariant.alpha,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        TableCell(
            modifier = Modifier.weight(DeviceTableWeights[1]),
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

        TableCell(
            modifier = Modifier.weight(DeviceTableWeights[2]),
            contentAlignment = Alignment.Center,
        ) {
            if (stale) {
                // Kolom jarak tidak relevan saat hilang -> tampilkan terakhir terlihat.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(device.lastSeen).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Sinyal hilang",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
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
}
