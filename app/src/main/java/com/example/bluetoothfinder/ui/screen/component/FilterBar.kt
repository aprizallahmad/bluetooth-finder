package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.bluetoothfinder.viewmodel.BluetoothFinderViewModel
import androidx.compose.foundation.layout.Column

/**
 * Bagian filter Screen 1:
 * - kolom pencarian berdasarkan nama perangkat / MAC address,
 * - ambang batas minimum sinyal (misal: hanya tampilkan perangkat ≥ -80 dBm).
 */
@Composable
fun FilterBar(
    query: String,
    minRssi: Int,
    onQueryChange: (String) -> Unit,
    onMinRssiChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Cari nama / MAC address") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Bersihkan pencarian")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* filter langsung saat mengetik */ }),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThresholdChip(
                label = "Semua sinyal",
                selected = minRssi == BluetoothFinderViewModel.NO_THRESHOLD,
                onClick = { onMinRssiChange(BluetoothFinderViewModel.NO_THRESHOLD) },
            )
            ThresholdChipOptions.forEach { threshold ->
                ThresholdChip(
                    label = "≥ $threshold dBm",
                    selected = minRssi == threshold,
                    onClick = { onMinRssiChange(threshold) },
                )
            }
        }
    }
}

private val ThresholdChipOptions = listOf(-90, -80, -70, -60)

@Composable
private fun ThresholdChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
