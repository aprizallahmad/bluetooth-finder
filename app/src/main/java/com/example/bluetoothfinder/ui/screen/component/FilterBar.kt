package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.bluetoothfinder.viewmodel.BluetoothFinderViewModel

/**
 * Bagian filter Screen 1:
 * - kolom pencarian berdasarkan nama perangkat / MAC address,
 * - ambang batas minimum sinyal (mis. hanya tampilkan perangkat ≥ -80 dBm),
 * - opsi menyembunyikan perangkat yang hilang sinyal (retensi: default tetap tampil redup).
 *
 * Filter diterapkan langsung saat mengetik / menekan chip (tidak perlu tombol terpisah).
 */
@Composable
fun FilterBar(
    query: String,
    minRssi: Int,
    hideStale: Boolean,
    onQueryChange: (String) -> Unit,
    onMinRssiChange: (Int) -> Unit,
    onHideStaleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val focusManager = LocalFocusManager.current

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
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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
                onClick = {
                    focusManager.clearFocus()
                    onMinRssiChange(BluetoothFinderViewModel.NO_THRESHOLD)
                },
            )
            ThresholdChipOptions.forEach { threshold ->
                ThresholdChip(
                    label = "≥ $threshold dBm",
                    selected = minRssi == threshold,
                    onClick = {
                        focusManager.clearFocus()
                        onMinRssiChange(threshold)
                    },
                )
            }
            ThresholdChip(
                label = "Sembunyikan hilang",
                selected = hideStale,
                onClick = {
                    focusManager.clearFocus()
                    onHideStaleChange(!hideStale)
                },
            )
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
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Filter aktif",
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}
