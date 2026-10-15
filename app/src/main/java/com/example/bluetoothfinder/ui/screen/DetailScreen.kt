package com.example.bluetoothfinder.ui.screen

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.bluetoothfinder.data.local.DeviceHistoryEntity
import com.example.bluetoothfinder.domain.BleDevice
import com.example.bluetoothfinder.domain.SignalCategory
import com.example.bluetoothfinder.ui.screen.component.RadarView
import com.example.bluetoothfinder.ui.screen.component.StatusBanner
import com.example.bluetoothfinder.ui.theme.color
import com.example.bluetoothfinder.viewmodel.RadarViewModel

/**
 * Screen 2: Detail Pelacakan (Radar View).
 *
 * Menampilkan visualisasi dinamis kedekatan perangkat target berdasarkan RSSI real-time,
 * indikator warna mengikuti kategori sinyal, serta detail informasi koneksi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    navController: NavHostController,
    viewModel: RadarViewModel = hiltViewModel(),
) {
    val target by viewModel.target.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isLost by viewModel.isLost.collectAsStateWithLifecycle()
    val bluetoothOn by viewModel.bluetoothOn.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel) {
        viewModel.onUiStarted()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onUiStarted()
                Lifecycle.Event.ON_STOP -> viewModel.onUiStopped()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    val deviceName = target?.name ?: history?.name ?: "Perangkat"
    val rssiValue = target?.smoothedRssi ?: history?.lastRssi
    val category = rssiValue?.let(SignalCategory::fromRssi) ?: SignalCategory.LOST

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = deviceName, maxLines = 1)
                        Text(
                            text = viewModel.address.ifBlank { "-" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (!bluetoothOn) {
                StatusBanner(
                    icon = Icons.Filled.BluetoothDisabled,
                    title = "Bluetooth nonaktif",
                    message = "Perangkat target tidak dapat dilacak sampai Bluetooth diaktifkan.",
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---------------- Radar ----------------
            RadarView(
                fraction = category.radarFraction,
                category = category,
                lost = isLost || target == null,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            // ---------------- Readout utama ----------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = category.color.copy(alpha = 0.12f),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (target == null) "Menunggu sinyal..." else category.distanceLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = category.color,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (target == null) "Target belum terdeteksi" else category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ReadoutItem(
                            label = "RSSI (raw)",
                            value = target?.let { "${it.rssi} dBm" } ?: "-- dBm",
                            valueColor = category.color,
                        )
                        ReadoutItem(
                            label = "Jarak",
                            value = if (target == null) "-- m" else category.distanceLabel,
                            valueColor = category.color,
                        )
                        ReadoutItem(
                            label = "Status",
                            value = if (isLost) "Lost" else "Live",
                            valueColor = if (isLost) SignalCategory.LOST.color else category.color,
                        )
                    }
                }
            }

            if (isLost) {
                StatusBanner(
                    icon = Icons.Filled.BluetoothDisabled,
                    title = "Sinyal Hilang (Lost)",
                    message = "Target di luar jangkauan atau tidak terdeteksi. " +
                        "Mendekatlah ke perangkat atau pastikan pemindaian berjalan.",
                    containerColor = SignalCategory.VERY_WEAK.color.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            // ---------------- Detail koneksi ----------------
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Informasi Koneksi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("Nama perangkat", deviceName)
                    InfoRow("Alamat (MAC / UUID)", viewModel.address.ifBlank { "-" })
                    InfoRow(
                        "RSSI mentah",
                        target?.let { "${it.rssi} dBm" } ?: history?.let { "${it.lastRssi} dBm" } ?: "-",
                    )
                    InfoRow(
                        "RSSI halus (EMA)",
                        target?.let { "${it.smoothedRssi} dBm" } ?: "-",
                    )
                    InfoRow(
                        "Tx Power",
                        target?.txPower?.let { "$it dBm" } ?: "Tidak tersedia",
                    )
                    InfoRow("Kategori sinyal", category.label)
                    InfoRow(
                        "Terakhir terdeteksi",
                        lastSeenLabel(target, history),
                    )
                    InfoRow(
                        "Total deteksi (riwayat)",
                        (history?.seenCount ?: 0).toString() + " kali",
                    )
                    InfoRow(
                        "Sinyal terkuat pernah tercatat",
                        history?.let { "${it.bestRssi} dBm" } ?: "-",
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReadoutItem(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun lastSeenLabel(target: BleDevice?, history: DeviceHistoryEntity?): String {
    val timestamp = target?.lastSeen ?: history?.lastSeenAt ?: return "-"
    return DateUtils.getRelativeTimeSpanString(timestamp).toString()
}
