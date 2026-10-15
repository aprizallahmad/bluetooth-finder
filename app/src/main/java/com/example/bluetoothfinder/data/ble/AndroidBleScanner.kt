package com.example.bluetoothfinder.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.example.bluetoothfinder.di.ApplicationScope
import com.example.bluetoothfinder.domain.BleDevice
import com.example.bluetoothfinder.util.BluetoothPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi [BleScanner] memakai [android.bluetooth.le.BluetoothLeScanner].
 *
 * Berjalan sebagai singleton agar riwayat hasil scan tidak hilang ketika
 * terjadi rotasi layar / perubahan konfigurasi.
 *
 * Ketahanan (resilience):
 * - Izin belum diberikan / Bluetooth mati / adapter null -> event error + status spesifik.
 * - `startScan`/`stopScan` di-wrap terhadap SecurityException & IllegalStateException
 *   (kasus umum saat izin dicabut atau Bluetooth dimatikan di tengah jalan).
 * - Perangkat yang tidak terlihat lebih dari [STALE_MS] dikeluarkan dari daftar.
 */
@Singleton
@SuppressLint("MissingPermission")
class AndroidBleScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) : BleScanner {

    private val adapter: BluetoothAdapter? by lazy {
        runCatching {
            context.getSystemService(BluetoothManager::class.java)?.adapter
        }.getOrNull()
    }

    private val _devices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    override val devices: StateFlow<Map<String, BleDevice>> = _devices.asStateFlow()

    private val _status = MutableStateFlow(ScanStatus.IDLE)
    override val status: StateFlow<ScanStatus> = _status.asStateFlow()

    // Optimistic default: nilai sebenarnya diisi saat refreshBluetoothState() pertama kali jalan.
    private val _bluetoothOn = MutableStateFlow(true)
    override val bluetoothOn: StateFlow<Boolean> = _bluetoothOn.asStateFlow()

    private val _errors = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var prunerJob: Job? = null
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleResult)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            prunerJob?.cancel()
            prunerJob = null
            _status.value = ScanStatus.IDLE
            emitError(scanFailedMessage(errorCode))
        }
    }

    override fun startScan() {
        if (isScanning) return

        val adapter = adapter
        if (adapter == null) {
            _status.value = ScanStatus.UNSUPPORTED
            emitError("Perangkat ini tidak mendukung Bluetooth Low Energy (BLE).")
            return
        }

        if (!BluetoothPermissions.hasAll(context)) {
            emitError("Izin Bluetooth belum diberikan. Izinkan akses untuk memindai perangkat.")
            return
        }

        refreshBluetoothState()
        if (!_bluetoothOn.value) {
            _status.value = ScanStatus.BLUETOOTH_OFF
            emitError("Bluetooth sedang nonaktif. Aktifkan Bluetooth untuk memindai.")
            return
        }

        val scanner = try {
            adapter.bluetoothLeScanner
        } catch (se: SecurityException) {
            emitError("Izin Bluetooth ditolak oleh sistem.")
            return
        }

        if (scanner == null) {
            _status.value = ScanStatus.BLUETOOTH_OFF
            emitError("Bluetooth sedang nonaktif. Aktifkan Bluetooth untuk memindai.")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (se: SecurityException) {
            emitError("Izin Bluetooth ditolak oleh sistem.")
            return
        } catch (ise: IllegalStateException) {
            emitError("Pemindaian tidak dapat dimulai, coba lagi beberapa saat.")
            return
        }

        isScanning = true
        _status.value = ScanStatus.SCANNING
        startPruner()
    }

    override fun stopScan() {
        if (!isScanning) return
        isScanning = false
        prunerJob?.cancel()
        prunerJob = null
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (se: SecurityException) {
            // Izin dicabut di tengah jalan: tetap anggap scan berhenti.
        } catch (ise: IllegalStateException) {
            // Bluetooth dimatikan di tengah jalan: scanner sudah null/tutup.
        }
        _status.value = ScanStatus.IDLE
    }

    override fun refreshBluetoothState() {
        val enabled = try {
            adapter?.isEnabled ?: false
        } catch (se: SecurityException) {
            return // izin belum ada: biarkan nilai lama, UI menampilkan banner izin.
        }
        _bluetoothOn.value = enabled
        if (!enabled && isScanning) {
            stopScan()
            _status.value = ScanStatus.BLUETOOTH_OFF
            emitError("Bluetooth dimatikan. Pemindaian dihentikan.")
        } else if (!enabled && _status.value == ScanStatus.SCANNING) {
            _status.value = ScanStatus.BLUETOOTH_OFF
        }
    }

    override fun isReady(): Boolean =
        BluetoothPermissions.hasAll(context) && _bluetoothOn.value && adapter != null

    // --------------------------------------------------------------------- internals

    private fun handleResult(result: ScanResult) {
        val previous = _devices.value[resolveAddress(result)]
        val rssi = result.rssi
        val smoothed = previous?.let {
            (it.smoothedRssi * SMOOTHING_ALPHA + rssi * (1 - SMOOTHING_ALPHA)).toInt()
        } ?: rssi

        // `ScanRecord.getTxPowerLevel()` bernilai Integer.MIN_VALUE bila tidak ada
        // dalam paket advertising (konstanta resminya tidak public).
        val txPower = result.scanRecord
            ?.txPowerLevel
            ?.takeIf { it != Integer.MIN_VALUE }

        val device = BleDevice(
            address = resolveAddress(result),
            name = resolveName(result) ?: previous?.name ?: BleDevice.UNKNOWN_NAME,
            rssi = rssi,
            smoothedRssi = smoothed,
            txPower = txPower,
            lastSeen = System.currentTimeMillis(),
            seenCount = (previous?.seenCount ?: 0) + 1,
        )

        _devices.value = _devices.value + (device.address to device)
    }

    private fun resolveAddress(result: ScanResult): String {
        // Beberapa perangkat menyembunyikan MAC -> pakai hash stabil sebagai fallback.
        val address = runCatching { result.device.address }.getOrNull()
        return address
            ?.takeIf { it.isNotBlank() && it != ANONYMOUS_ADDRESS }
            ?: "anon-${System.identityHashCode(result.device)}"
    }

    private fun resolveName(result: ScanResult): String? {
        // getNamedDevice() butuh BLUETOOTH_CONNECT di Android 12+.
        val deviceName = runCatching { result.device.name }.getOrNull()
        return deviceName ?: result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
    }

    private fun startPruner() {
        if (prunerJob != null) return
        prunerJob = scope.launch {
            while (isActive) {
                delay(PRUNE_INTERVAL_MS)
                if (!isScanning) continue
                val now = System.currentTimeMillis()
                _devices.value = _devices.value.filterValues {
                    now - it.lastSeen < STALE_MS
                }
            }
        }
    }

    private fun emitError(message: String) {
        _errors.tryEmit(message)
    }

    private fun scanFailedMessage(errorCode: Int): String = when (errorCode) {
        1 -> "Pemindaian sudah berjalan di perangkat."
        2 -> "Pemindaian diblokir sistem. Cek apakah Bluetooth mati atau aplikasi di latar belakang."
        3 -> "Terjadi kesalahan internal Bluetooth."
        4 -> "Mode pemindaian tidak didukung perangkat ini."
        else -> "Pemindaian gagal (kode $errorCode)."
    }

    private companion object {
        /** Bobot RSSI terbaru pada perhitungan EMA. */
        const val SMOOTHING_ALPHA = 0.35f

        /** Perangkat yang tidak terlihat selama ini dikeluarkan dari daftar. */
        const val STALE_MS = 15_000L

        const val PRUNE_INTERVAL_MS = 2_000L

        const val ANONYMOUS_ADDRESS = "02:00:00:00:00:00"
    }
}
