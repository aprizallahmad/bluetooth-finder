package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * Header tabel daftar perangkat: Nama+MAC, RSSI mentah, dan estimasi jarak.
 * Bobot kolom dan garisnya sama persis dengan setiap baris data ([DeviceRow]).
 */
@Composable
fun HeaderData() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .deviceTableGrid(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TitleText(
            text = "Perangkat",
            modifier = Modifier.weight(DeviceTableWeights[0]),
            fontWeight = FontWeight.SemiBold,
        )
        TitleText(
            text = "RSSI",
            modifier = Modifier.weight(DeviceTableWeights[1]),
            fontWeight = FontWeight.SemiBold,
        )
        TitleText(
            text = "Jarak",
            modifier = Modifier.weight(DeviceTableWeights[2]),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
