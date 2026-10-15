package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Header tabel daftar perangkat: Nama+MAC, RSSI mentah, dan estimasi jarak.
 */
@Composable
fun HeaderData() {
    Column {
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TitleText(text = "Perangkat", modifier = Modifier.weight(1.4f))
            TitleText(text = "RSSI", modifier = Modifier.weight(0.8f))
            TitleText(text = "Jarak", modifier = Modifier.weight(1.2f))
        }
    }
}
