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
import com.example.bluetoothfinder.ui.screen.TitleText


@Composable
fun HeaderData() {
    Column() {
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TitleText(text = "RSSI", modifier = Modifier.weight(1f))
            TitleText(text = "Distance", modifier = Modifier.weight(1f))
            TitleText(text = "Signal Strength", modifier = Modifier.weight(1f))
        }
    }
}

