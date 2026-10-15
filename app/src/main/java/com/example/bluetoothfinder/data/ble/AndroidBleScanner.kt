package com.example.bluetoothfinder.data.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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
import java.util.UUID
import java.util.concurrent.TimeoutException
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
 *
 * Throttle tampilan: paket advertising yang datang sangat cepat ditampung di buffer
 * [deviceStates], lalu di-flush ke [_devices] (yang dibaca UI) setiap
 * [UI_REFRESH_INTERVAL_MS] agar data live scanning tidak bergetar terlalu cepat.
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

    /**
     * State perangkat yang diperbarui setiap paket advertising masuk (cepat),
     * sedangkan [_devices] (yang dibaca UI) hanya di-flush dari sini secara berkala
     * setiap [UI_REFRESH_INTERVAL_MS] agar tampilan tidak bergetar.
     */
    private val deviceStates = mutableMapOf<String, BleDevice>()

    /** Cache nama perangkat dari berbagai sumber (advertising/bonded) agar tidak hilang. */
    private val knownNames = mutableMapOf<String, String>()

    /** Nama perangkat yang sudah di-bond/pair (dibaca sekali saat scan dimulai). */
    private var bondedNames: Map<String, String> = emptyMap()

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

    private var flushJob: Job? = null
    private var isScanning = false

    // --------------------------------------------------------- GATT name reading

    private var gatt: BluetoothGatt? = null
    private var nameReadCallback: ((Result<String>) -> Unit)? = null
    private var nameReadJob: Job? = null
    private var nameTargetAddress: String? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleResult)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            stopFlushTicker()
            flushDeviceStates(pruneStale = false)
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

        // Muat nama perangkat yang sudah pernah di-bond/pair (butuh BLUETOOTH_CONNECT).
        refreshBondedNames()

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
        startFlushTicker()
    }

    override fun stopScan() {
        if (!isScanning) return
        isScanning = false
        stopFlushTicker()
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (se: SecurityException) {
            // Izin dicabut di tengah jalan: tetap anggap scan berhenti.
        } catch (ise: IllegalStateException) {
            // Bluetooth dimatikan di tengah jalan: scanner sudah null/tutup.
        }
        // Tampilkan hasil terakhir (maksimal [UI_REFRESH_INTERVAL_MS] paket lalu).
        flushDeviceStates(pruneStale = false)
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
        }
    }

    override fun isReady(): Boolean =
        BluetoothPermissions.hasAll(context) && _bluetoothOn.value && adapter != null

    // --------------------------------------------------------------------- internals

    private fun handleResult(result: ScanResult) {
        val address = resolveAddress(result)
        val previous = deviceStates[address]
        val rssi = result.rssi
        val smoothed = previous?.let {
            (it.smoothedRssi * SMOOTHING_ALPHA + rssi * (1 - SMOOTHING_ALPHA)).toInt()
        } ?: rssi

        // `ScanRecord.getTxPowerLevel()` bernilai Integer.MIN_VALUE bila tidak ada
        // dalam paket advertising (konstanta resminya tidak public).
        val txPower = result.scanRecord
            ?.txPowerLevel
            ?.takeIf { it != Integer.MIN_VALUE }

        // Jangan menimpa nama yang sudah diketahui dengan nama kosong.
        val resolvedName = resolveName(result)
        val name = resolvedName
            ?: previous?.name?.takeIf { it.isNotBlank() }
            ?: BleDevice.UNKNOWN_NAME
        if (resolvedName != null) knownNames[address] = resolvedName

        val device = BleDevice(
            address = address,
            name = name,
            rssi = rssi,
            smoothedRssi = smoothed,
            txPower = txPower,
            lastSeen = System.currentTimeMillis(),
            seenCount = (previous?.seenCount ?: 0) + 1,
        )

        // Ditulis cepat ke buffer; UI hanya dibarui oleh ticker flush (throttle).
        deviceStates[device.address] = device
    }

    private fun resolveAddress(result: ScanResult): String {
        // Beberapa perangkat menyembunyikan MAC -> pakai hash stabil sebagai fallback.
        val address = runCatching { result.device.address }.getOrNull()
        return address
            ?.takeIf { it.isNotBlank() && it != ANONYMOUS_ADDRESS }
            ?: "anon-${System.identityHashCode(result.device)}"
    }

    /**
     * Resolusi nama perangkat dari sumber yang paling bisa dipercaya lebih dulu:
     * 1. **Nama dari paket advertising** (`ScanRecord`) — ini yang paling umum untuk perangkat BLE.
     * 2. Nama perangkat yang sudah pernah di-bond/pair (pairing list).
     * 3. Cache nama lokal dari hasil scan sebelumnya.
     * 4. `BluetoothDevice.getName()` (membutuhkan izin BLUETOOTH_CONNECT).
     *
     * Catatan: tidak semua perangkat mengiklankan namanya (mis. iPhone/Android yang
     * sedang idle, iBeacon) — sesuai brief, nama ditampilkan "jika ada/tersedia".
     */
    private fun resolveName(result: ScanResult): String? {
        result.scanRecord
            ?.deviceName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val address = resolveAddress(result)
        bondedNames[address]?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        knownNames[address]?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        return runCatching { result.device.name }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    /** Membaca daftar perangkat yang sudah di-bond/pair sebagai sumber nama tambahan. */
    private fun refreshBondedNames() {
        val bonded = runCatching { adapter?.bondedDevices }.getOrNull() ?: return
        val mapped = bonded
            .mapNotNull { device ->
                val name = runCatching { device.name }.getOrNull()?.trim()
                val address = runCatching { device.address }.getOrNull()
                if (name.isNullOrBlank() || address.isNullOrBlank()) null else address to name
            }
            .toMap()
        if (mapped.isNotEmpty()) bondedNames = bondedNames + mapped
    }

    /**
     * Ticker flush: memindahkan isi [deviceStates] ke [_devices] setiap
     * [UI_REFRESH_INTERVAL_MS] sehingga layar maksimal terbarui 1× per interval,
     * walau paket advertising datang jauh lebih cepat (~10× per detik pada LOW_LATENCY).
     * Sekaligus membuang perangkat yang tidak terlihat lebih dari [STALE_MS].
     */
    private fun startFlushTicker() {
        if (flushJob != null) return
        flushJob = scope.launch {
            while (isActive) {
                delay(UI_REFRESH_INTERVAL_MS)
                if (!isScanning) continue
                flushDeviceStates(pruneStale = true)
            }
        }
    }

    private fun stopFlushTicker() {
        flushJob?.cancel()
        flushJob = null
    }

    private fun flushDeviceStates(pruneStale: Boolean) {
        if (pruneStale) {
            val now = System.currentTimeMillis()
            deviceStates.entries.removeAll { now - it.value.lastSeen >= STALE_MS }
        }
        _devices.value = deviceStates.toMap()
    }

    /**
     * Membaca nama perangkat via koneksi GATT (service Generic Access `0x1800`,
     * characteristic Device Name `0x2A00`). Hasilnya persis nama di Pengaturan ->
     * Bluetooth perangkat, sehingga tetap terbaca meski perangkat tidak broadcast
     * namanya (kasus HP Android/iOS). Selalu menutup koneksi setelah selesai.
     */
    override fun readDeviceName(address: String, onResult: (Result<String>) -> Unit) {
        val adapter = adapter
        if (adapter == null || !BluetoothPermissions.hasAll(context)) {
            onResult(Result.failure(IllegalStateException("Bluetooth atau izin tidak siap.")))
            return
        }
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            onResult(Result.failure(e))
            return
        }

        // Satu koneksi pada satu waktu: batal pembacaan sebelumnya bila ada.
        nameReadCallback?.invoke(
            Result.failure(IllegalStateException("Pembacaan nama lain sedang berlangsung."))
        )
        closeGatt()
        nameReadCallback = onResult
        nameTargetAddress = address

        // Batas waktu agar layar tidak menunggu tanpa kejelasan.
        nameReadJob = scope.launch {
            delay(NAME_READ_TIMEOUT_MS)
            finishNameRead(Result.failure(TimeoutException("Waktu membaca nama perangkat habis.")))
        }

        gatt = try {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (se: SecurityException) {
            finishNameRead(Result.failure(se))
            null
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when {
                status != BluetoothGatt.GATT_SUCCESS -> finishNameRead(
                    Result.failure(IllegalStateException("Koneksi ke perangkat gagal (status $status)."))
                )

                newState == BluetoothProfile.STATE_CONNECTED -> g.discoverServices()

                newState == BluetoothProfile.STATE_DISCONNECTED -> finishNameRead(
                    Result.failure(IllegalStateException("Perangkat terputus sebelum nama terbaca."))
                )
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                finishNameRead(
                    Result.failure(IllegalStateException("Gagal menemui service perangkat."))
                )
                return
            }
            val characteristic = g.getService(DEVICE_NAME_SERVICE_UUID)
                ?.getCharacteristic(DEVICE_NAME_CHAR_UUID)
            if (characteristic == null) {
                finishNameRead(
                    Result.failure(IllegalStateException("Perangkat tidak menyediakan karakteristik nama."))
                )
                return
            }
            @Suppress("DEPRECATION")
            if (!g.readCharacteristic(characteristic)) {
                finishNameRead(
                    Result.failure(IllegalStateException("Gagal memulai pembacaan nama."))
                )
            }
        }

        // Signature lama dipertahankan agar kompatibel dari API 24; pada API 33+
        // sistem meneruskan panggilan ke method ini bila override baru tidak ditulis.
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                finishNameRead(
                    Result.failure(IllegalStateException("Gagal membaca nama (status $status)."))
                )
                return
            }
            val name = characteristic.value
                ?.toString(Charsets.UTF_8)
                ?.trim()
                .orEmpty()
            if (name.isBlank()) {
                finishNameRead(Result.failure(IllegalStateException("Nama perangkat kosong.")))
            } else {
                finishNameRead(Result.success(name))
            }
        }
    }

    /**
     * Menyelesaikan satu siklus pembacaan nama: tutup koneksi, simpan nama hasil
     * baca ke cache (agar daftar & riwayat ikut menampilkan nama), lalu panggil callback.
     */
    private fun finishNameRead(result: Result<String>) {
        val callback = nameReadCallback ?: return
        val address = nameTargetAddress
        nameReadCallback = null
        nameTargetAddress = null
        nameReadJob?.cancel()
        nameReadJob = null
        closeGatt()

        scope.launch {
            if (address != null) {
                result.onSuccess { name ->
                    knownNames[address] = name
                    deviceStates[address]?.let { current ->
                        deviceStates[address] = current.copy(name = name)
                        _devices.value = deviceStates.toMap()
                    }
                }
            }
            callback(result)
        }
    }

    private fun closeGatt() {
        runCatching { gatt?.close() }
        gatt = null
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

        /**
         * Interval throttle tampilan live scan: UI maksimal dibarui 1× per detik,
         * walau paket advertising datang jauh lebih sering (SCAN_MODE_LOW_LATENCY).
         */
        const val UI_REFRESH_INTERVAL_MS = 1_000L

        /** Batas waktu membaca nama perangkat via koneksi GATT. */
        const val NAME_READ_TIMEOUT_MS = 8_000L

        /** Service Generic Access (wajib ada di setiap perangkat GATT). */
        private val DEVICE_NAME_SERVICE_UUID: UUID =
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")

        /** Characteristic Device Name: persis nama di Pengaturan -> Bluetooth perangkat. */
        private val DEVICE_NAME_CHAR_UUID: UUID =
            UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")

        const val ANONYMOUS_ADDRESS = "02:00:00:00:00:00"
    }
}
