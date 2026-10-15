package com.example.bluetoothfinder.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.example.bluetoothfinder.data.ble.ScanStatus
import com.example.bluetoothfinder.ui.screen.component.BotBar
import com.example.bluetoothfinder.ui.screen.component.DeviceRow
import com.example.bluetoothfinder.ui.screen.component.FilterBar
import com.example.bluetoothfinder.ui.screen.component.HeaderData
import com.example.bluetoothfinder.ui.screen.component.StatusBanner
import com.example.bluetoothfinder.util.BluetoothPermissions
import com.example.bluetoothfinder.viewmodel.BluetoothFinderViewModel
import android.bluetooth.BluetoothAdapter
import android.net.Uri

/**
 * Screen 1: Dashboard Utama (Scanner).
 *
 * Pusat kontrol pemindaian: Start/Stop, pencarian, filter ambang RSSI,
 * daftar perangkat real-time yang diurutkan dari sinyal terkuat, serta
 * banner penanganan untuk izin ditolak / Bluetooth nonaktif.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: BluetoothFinderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
    val bluetoothOn by viewModel.bluetoothOn.collectAsStateWithLifecycle()
    val hasPermissions by viewModel.hasPermissions.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val minRssi by viewModel.minRssi.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val isScanning = scanStatus == ScanStatus.SCANNING

    // ---- App lifecycle: jeda scan saat ke latar belakang, lanjutkan saat kembali.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel) {
        // Sinkronisasi awal (observer baru menerima event berikutnya saja).
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

    // ---- Error / edge-case dari repository ditampilkan sebagai snackbar.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    // ---- Launcher izin & intent mengaktifkan Bluetooth.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onPermissionsResult(result.values.all { it })
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onBluetoothEnabled()
    }

    val onStartClick = {
        when {
            !hasPermissions -> permissionLauncher.launch(BluetoothPermissions.required())
            !bluetoothOn -> enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> viewModel.startScan()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Bluetooth Finder")
                        Text(
                            text = statusLabel(scanStatus, devices.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Filled.History, contentDescription = "Riwayat perangkat")
                    }
                },
            )
        },
        bottomBar = {
            BotBar(
                onStopClick = viewModel::stopScan,
                onScanClick = onStartClick,
                startEnabled = !isScanning,
                stopEnabled = isScanning,
                startLabel = if (isScanning) "Scanning..." else "Scan",
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            if (!hasPermissions) {
                StatusBanner(
                    icon = Icons.Filled.Lock,
                    title = "Izin Bluetooth diperlukan",
                    message = if (BluetoothPermissions.isLegacyLocationBased)
                        "Aplikasi membutuhkan izin lokasi untuk memindai perangkat BLE di sekitar."
                    else
                        "Aplikasi membutuhkan izin Bluetooth untuk memindai perangkat di sekitar.",
                    actionLabel = "Izinkan",
                    onAction = { permissionLauncher.launch(BluetoothPermissions.required()) },
                )
            } else if (!bluetoothOn) {
                StatusBanner(
                    icon = Icons.Filled.BluetoothDisabled,
                    title = "Bluetooth nonaktif",
                    message = "Aktifkan Bluetooth untuk memindai perangkat di sekitar.",
                    actionLabel = "Aktifkan",
                    onAction = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    },
                )
            }

            FilterBar(
                query = query,
                minRssi = minRssi,
                onQueryChange = viewModel::onQueryChange,
                onMinRssiChange = viewModel::onMinRssiChange,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (devices.isEmpty()) {
                EmptyScanState(
                    hasPermissions = hasPermissions,
                    bluetoothOn = bluetoothOn,
                    isScanning = isScanning,
                    filtered = query.isNotBlank() || minRssi != BluetoothFinderViewModel.NO_THRESHOLD,
                    onClearFilters = viewModel::clearFilters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 12.dp),
                ) {
                    item(key = "header") { HeaderData() }
                    items(devices, key = { it.macAddress }) { device ->
                        DeviceRow(
                            device = device,
                            onClick = {
                                navController.navigate("detail/${Uri.encode(device.macAddress)}")
                            },
                        )
                    }
                    item(key = "footer") {
                        Text(
                            text = "${devices.size} perangkat ditampilkan (urut RSSI terkuat)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: ScanStatus, count: Int): String = when (status) {
    ScanStatus.SCANNING -> "Memindai... $count perangkat terdeteksi"
    ScanStatus.BLUETOOTH_OFF -> "Bluetooth nonaktif"
    ScanStatus.UNSUPPORTED -> "BLE tidak didukung"
    ScanStatus.IDLE -> "Pemindaian berhenti"
}

@Composable
private fun EmptyScanState(
    hasPermissions: Boolean,
    bluetoothOn: Boolean,
    isScanning: Boolean,
    filtered: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when {
        !hasPermissions -> "Berikan izin Bluetooth terlebih dahulu untuk mulai memindai."
        !bluetoothOn -> "Bluetooth sedang nonaktif. Aktifkan Bluetooth lalu tekan tombol Scan."
        filtered -> "Tidak ada perangkat yang cocok dengan filter saat ini."
        isScanning -> "Mencari perangkat di sekitar...\nBelum ada perangkat terdeteksi."
        else -> "Tekan tombol Scan untuk mulai mencari perangkat Bluetooth di sekitar."
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (filtered) {
                androidx.compose.material3.TextButton(onClick = onClearFilters) {
                    Text("Bersihkan filter", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
